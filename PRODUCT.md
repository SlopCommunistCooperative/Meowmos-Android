# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

Meowmos primarily serves families and small groups that use a self-hosted Memos instance to keep shared notes and discuss them. Personal note-taking, including on-device use without a server, is a secondary use case.

## Product Purpose

Members can put a note in the right shared space, find it later, and continue the discussion in its comments. A successful post goes to the intended space and audience. A successful reply stays attached to its note.

## Positioning

Meowmos is an independent Android client for Memos. It aims to cover the core workflows of the Memos web app using native Android interactions rather than copying its layout. Users choose their own Memos server or keep their personal notes on the device.

## Operating Context

Users write Markdown notes, add images or files, and organize notes with tags and pinning. They can find notes through search, read comments below a note, and preview comments or reply from Home and Explore. Android's share sheet and home-screen widget provide entry points for personal capture.

## Capabilities and Constraints

- Notes can be written offline and synchronized when the connection returns. Posting comments requires a connection. Local accounts do not require a server and do not expose server collaboration features.
- Spaces require server support. Space and audience are distinct choices. Meowmos remembers the last space for new notes and the last explicitly selected comment audience separately for each account. The remembered comment audience does not automatically widen the parent note's audience.
- Keep local-only use and server-version differences explicit. [README.md](README.md) lists the supported Memos versions. Meowmos does not collect user data.

## Brand Commitments

The app is named Meowmos. It is an independent fork of Moe Memos Android and is not affiliated with the Memos server project.

## Evidence on Hand

The repository includes the [app icon](fastlane/metadata/android/en-US/images/icon.png), [phone screenshots](fastlane/metadata/android/en-US/images/phoneScreenshots), and [tablet screenshots](fastlane/metadata/android/en-US/images/sevenInchScreenshots). The store screenshots show earlier app screens and do not document current Spaces or Comments behavior.

## Product Principles

1. Prioritize shared workflows: post to a space, find a note, and reply in context.
2. Make the posting destination and audience clear before publishing. Reuse preferences without silently broadening access.
3. Match the core workflows of Memos web where the server supports them. Use Android conventions for how people complete those workflows.
4. Keep personal capture useful without connectivity or a server. Do not imply that a comment has been posted before the server accepts it.
