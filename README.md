# Meowmos

[![justforfunnoreally.dev badge](https://img.shields.io/badge/justforfunnoreally-dev-9ff)](https://justforfunnoreally.dev)

<img alt="Meowmos" src="fastlane/metadata/android/en-US/images/icon.png" width="160" height="160" />

Meowmos is an Android app for writing and organizing notes.

Use Meowmos with a self-hosted [Memos](https://github.com/usememos/memos) server or locally on your device.

Meowmos supports Memos 0.21.0, 0.27.0 through 0.31.0, and 26.10 through 26.11.2. Newer releases can break compatibility. [Mortis](https://github.com/mudkipme/mortis) can translate newer APIs to the Memos 0.21.0 API.

## Installation

Meowmos requires Android 15 (API 35) or later. Download the APK from [Meowmos releases](https://github.com/SlopCommunistCooperative/Meowmos-Android/releases).

## Features

- Back up and restore local notes.
- Write offline and sync when you're back online.
- On servers that support Spaces, use the menu next to Post to choose a space you belong to. Meowmos remembers that choice for new notes on the same account. Until you choose a space, new notes have no space. Selecting a space defaults the note's visibility to its members.
- On supported Memos servers, read comments below a note, or preview them and reply directly from Home and Explore. Posting requires a connection. The editor starts with the note's audience or a compatible audience you last selected on that account.
- Meowmos uses Material 3 Expressive and supports themed app icons. To use the Meowmos palette instead of system colors, turn off **Material You colors** under Preferences in Settings.
- Write and preview Markdown, add images and files, and check off tasks.
- Find notes with search and tags. Pin notes you want to keep at the top.
- Open notes from the home screen widget. Share text, images, and web links to Meowmos from other apps.
- See your note activity in a graph.
- Meowmos does not collect your data.

Meowmos is an independent fork of [Moe Memos Android](https://github.com/mudkipme/MoeMemosAndroid) and is not affiliated with the Memos server project.

## Development

Meowmos is built with Kotlin and Jetpack Compose.

## License

Meowmos is licensed under [GPLv3](LICENSE).
