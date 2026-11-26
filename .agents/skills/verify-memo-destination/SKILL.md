---
name: verify-memo-destination
description: Check that the Meowmos editor waits for the posting destination before saving. Run this when changing remembered Memos Spaces, initialization, Post, Back, Close, autosave, or the background save.
---

# Verify memo destination readiness

A new editor with content must not write a note until it knows where the note belongs. An early write can use the account's public default instead of the remembered family space.

## Run

Run this from the repository root with Python 3.11 or newer. No packages are required.

```sh
python3 .agents/skills/verify-memo-destination/scripts/check.py --self-test
```

- `PASS` means the property held in every reachable state in the model.
- `CAUGHT` means the self-test found a shortest counterexample after removing a guard from an in-memory copy of the Kotlin source.
- `FAIL` means the checker found a counterexample without changing the source. Replay the steps in the editor to confirm the bug.
- `REVIEW` means the checker could not map the source to the model or reach a normal save path. Treat it as inconclusive.

## Scope and code mapping

- In [MemoInputPage.kt](../../../app/src/main/java/io/github/slopcommunistcooperative/meowmos/ui/page/memoinput/MemoInputPage.kt), `LaunchedEffect(Unit)` reads the remembered space. If it finds one, it checks membership before setting `initialized`. The model pauses at both reads and treats assignments between them as one main-thread step. If the server confirms the space is unavailable, the editor switches to a private note with no space. A network failure keeps the remembered space.
- The checker covers `submit`, `handleExit`, the autosave `LaunchedEffect`, and the `ON_STOP` observer. It reads the autosave and lifecycle guards from Kotlin and checks how the toolbar connects to save callbacks. In the model, `ON_STOP` can only write after autosave marks the note dirty. Removing the `ON_STOP` readiness guard alone cannot cause an early write.
- In [MemoInputComponents.kt](../../../app/src/main/java/io/github/slopcommunistcooperative/meowmos/ui/page/memoinput/MemoInputComponents.kt), `MemoInputTopBar` gates Post and dispatches Back and Close through the same queued request. The checker reads both guards. The model allows attempts after navigation would have started so navigation timing cannot hide an early write.

The model covers one new remote-account editor with text or an attachment, a public account default, autosave on or off, and zero or one remembered space. It explores each reachable state once and checks that every write path can still save to the Family space. It does not verify Compose scheduling, process recreation, account switches, manual space changes, drafts, Room, sync, or server payloads. Run `UiComponentsTest.backWaitsForTheRememberedSpaceBeforeSaving` on Android to check Back dispatch in Compose.

## Maintenance

The working tree was based on commit `0553999b96a634b75239ea7addd5ad3c1c90cf75`. The checked editor sources were uncommitted. SHA-256 fingerprints:

- `MemoInputPage.kt`: `2ccb653af56dbb10b9cf87511a1d628b4c5d23d90cc66b77c6da15d4471f9858`
- `MemoInputComponents.kt`: `ac0f672f3e29284d75a8ea58882c22589341b46909cd94ef7b3c32e9e25925f4`

After changing either file, compare it with this record and trace every save path. Check that each modeled step still matches the Kotlin suspension points and callbacks. If the model no longer matches, use `/maintain-formal-verification-skill`. Run the check and its self-test before recording a new sync commit or fingerprint. Do not weaken the property to make a counterexample disappear.
