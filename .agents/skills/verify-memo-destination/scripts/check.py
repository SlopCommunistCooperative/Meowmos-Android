#!/usr/bin/env python3
"""Explore reachable save events using the guards in the Kotlin editor."""

import argparse
from collections import deque
from dataclasses import dataclass, replace
from pathlib import Path
import re
import sys


EDITOR = Path("app/src/main/java/io/github/slopcommunistcooperative/meowmos/ui/page/memoinput")
POST = r'label = stringResource\(R\.string\.post\),\s*enabled = ([^,]+),\s*onClick = onSubmit'
CLOSE = r'LaunchedEffect\(initialized, closeRequested\)\s*\{\s*if \(([^\n]+)\)\s*\{\s*closeRequested = false\s*onClose\(\)'
AUTOSAVE = r'LaunchedEffect\(initialized, autosaveEnabled,[^\n]+\n\s*if \(([^\n]+)\) \{\s*return@LaunchedEffect'
STOP = r'if \(([^\n]*Lifecycle\.Event\.ON_STOP[^\n]*)\) \{\s*viewModel\.flushAutosaveInBackground\('
PUBLISH_CALL = r'viewModel\.(?:createMemo|editMemo|autosave|flushAutosave|flushAutosaveInBackground)\('


class SourceDrift(Exception):
    pass


def capture(source, pattern):
    matches = list(re.finditer(pattern, source))
    if len(matches) != 1:
        raise SourceDrift(f"expected one source match: {pattern}")
    return matches[0][1].strip()


def check_wiring(page, bar):
    expected = (
        (page, r'var initialized by remember \{ mutableStateOf\(false\) \}'),
        (page, r'initialized = initialized,\s*canSubmit = [^\n]+,\s*onClose = \{ handleExit\(\) \},\s*onSubmit = \{ submit\(\) \}'),
        (page, r'if \(!editorRestored && memoIdentifier == null && initialRow == null && lastSpace != null\) \{\s*currentSpace = lastSpace\.name\s*currentVisibility = MemoVisibility\.SPACE\s*val available = viewModel\.listSpaces\(\)\s*if \(available != null\) \{\s*spaces = available\s*if \(available\.none \{ it\.name == lastSpace\.name \}\) \{\s*currentSpace = null\s*currentVisibility = MemoVisibility\.PRIVATE'),
        (page, r'if \(!editorRestored\) initialVisibility = currentVisibility\s*editorRestored = true\s*initialized = true'),
        (bar, r'var closeRequested by rememberSaveable \{ mutableStateOf\(false\) \}\s*val requestClose: \(\) -> Unit = \{ closeRequested = true \}\s*BackHandler\(onBack = requestClose\)'),
        (bar, r'label = stringResource\(R\.string\.close\), onClick = requestClose'),
    )
    for source, pattern in expected:
        if len(re.findall(pattern, source)) != 1:
            raise SourceDrift(f"review callback or destination wiring: {pattern}")
    lookup = page.index("val lastSpace = viewModel.lastMemoSpace(accountKey)")
    validated = page.index("if (available.none { it.name == lastSpace.name })")
    ready = page.index("initialized = true")
    if not lookup < validated < ready or page.count("initialized = true") != 1:
        raise SourceDrift("editor readiness moved relative to destination lookup")
    if len(re.findall(PUBLISH_CALL, page)) != 6:
        raise SourceDrift("editor has a different set of memo-writing call sites")


def guard(expression, values):
    def term(token):
        token = token.strip()
        atom = token[1:] if token.startswith("!") else token
        if atom not in values:
            raise SourceDrift(f"unsupported guard term: {token}")
        return values[atom] != token.startswith("!")

    return any(all(part) for part in [
        [term(token) for token in conjunction.split("&&")]
        for conjunction in expression.split("||")
    ])


@dataclass(frozen=True)
class State:
    phase: str = "loading preference"
    destination: str = "no space / PUBLIC"
    close_requested: bool = False
    dirty: bool = False


def explore(page, bar):
    check_wiring(page, bar)
    post, close, autosave, stop = (
        capture(source, pattern) for source, pattern in
        ((bar, POST), (bar, CLOSE), (page, AUTOSAVE), (page, STOP))
    )
    queue = deque((State(), enabled, ()) for enabled in (False, True))
    seen = set()
    writes = set()
    while queue:
        state, autosave_enabled, trace = queue.popleft()
        if (state, autosave_enabled) in seen:
            continue
        seen.add((state, autosave_enabled))
        initialized = state.phase == "ready"
        values = {
            "initialized": initialized, "canSubmit": True,
            "closeRequested": state.close_requested, "autosaveEnabled": autosave_enabled,
            "autosaveDirty": state.dirty, "exiting": False,
            "event == Lifecycle.Event.ON_STOP": True,
        }
        transitions = []
        if state.phase == "loading preference":
            transitions += [
                ("read account preference: no saved space", replace(state, phase="ready")),
                ("read account preference: Family", replace(state, phase="loading memberships", destination="Family / SPACE")),
            ]
        elif state.phase == "loading memberships":
            transitions += [
                ("membership confirmed or network unavailable", replace(state, phase="ready")),
                ("membership revoked", replace(state, phase="ready", destination="no space / PRIVATE")),
            ]
        for event in ("Android Back", "toolbar Close"):
            transitions.append((event, replace(state, close_requested=True)))
        if guard(post, values):
            writes.add(("Post", state.destination))
            if not initialized:
                return seen, writes, (*trace, f"Post saves to {state.destination} before readiness")
        if guard(close, values):
            if autosave_enabled:
                writes.add(("Back/Close", state.destination))
                if not initialized:
                    return seen, writes, (*trace, f"Back/Close saves to {state.destination} before readiness")
            transitions.append(("dispatch Back/Close", replace(state, close_requested=False)))
        if not guard(autosave, values):
            writes.add(("autosave", state.destination))
            if not initialized:
                return seen, writes, (*trace, f"autosave saves to {state.destination} before readiness")
            transitions.append(("autosave queues a memo", replace(state, dirty=True)))
        if guard(stop, values):
            writes.add(("ON_STOP", state.destination))
            if not initialized:
                return seen, writes, (*trace, f"ON_STOP saves to {state.destination} before readiness")
        queue.extend((after, autosave_enabled, (*trace, event)) for event, after in transitions)
    return seen, writes, None


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--self-test", action="store_true", help="also remove each primary guard from a scratch copy of the Kotlin source")
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[4]
    page = (root / EDITOR / "MemoInputPage.kt").read_text(encoding="utf-8")
    bar = (root / EDITOR / "MemoInputComponents.kt").read_text(encoding="utf-8")
    try:
        states, writes, violation = explore(page, bar)
        if violation:
            print("FAIL: " + " -> ".join(violation), file=sys.stderr)
            return 1
        required = {(event, "Family / SPACE") for event in ("Post", "Back/Close", "autosave", "ON_STOP")}
        if not required <= writes:
            raise SourceDrift("the model no longer reaches normal Family saves on every path")
        print(f"PASS: no save before destination readiness in {len(states)} reachable states")
        if args.self_test:
            variants = (
                (page, bar.replace("initialized && canSubmit", "canSubmit", 1)),
                (page, bar.replace("initialized && closeRequested", "closeRequested", 1)),
                (page.replace("!initialized || !autosaveEnabled", "!autosaveEnabled", 1), bar),
            )
            for (scratch_page, scratch_bar), event in zip(variants, ("Post", "Back/Close", "autosave")):
                _, _, counterexample = explore(scratch_page, scratch_bar)
                if not counterexample or not counterexample[-1].startswith(event + " saves"):
                    print(f"FAIL: removing the {event} guard did not expose a premature save", file=sys.stderr)
                    return 1
                print("CAUGHT: " + " -> ".join(counterexample))
    except SourceDrift as error:
        print(f"REVIEW: {error}", file=sys.stderr)
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
