[查看此文档的中文版本](README.md)

# System Shell Box

## What is this?

**System Shell Box (SSB)** is a free and open-source file management tool inspired by MT Manager, but with core functionalities fully implemented via a terminal emulator.  
📦 This project aims to provide users with a **lightweight and extensible** alternative to MT Manager's paid features, without relying on closed-source components.

**Note**: This app is currently in early development and lacks many features.

## How to Build?

### Steps
1. Clone the repository:
   ```bash
   git clone https://github.com/kgultrt/SystemShellBox
   ```

2. Modify Gradle configuration:  
   Open the `gradle.properties` file in the project root and **delete the following line**:
   ```properties
   # 专门管环境设置，请删除这一行
   android.aapt2FromMavenOverride=/data/data/com.termux/files/home/.androidide/aapt2
   ```

   Also delete the `local.properties` file.

3. Build using Android Studio or command line (do NOT use b.sh):
   ```bash
   ./gradlew assembleDebug
   ```

## How to Contribute?

⚠️ **Important**: This repository **currently does NOT accept external Pull Requests**. If you need custom modifications, please download the source code directly or fork the repository.

☕ Buy me a coffee: Not required, but greatly appreciated if you'd like to support the project. (Donation methods not yet available)

## License

This project is licensed under the [GNU General Public License v3.0](LICENSE.txt) (GPLv3).

You are free to use, modify, and distribute this software, provided that:
- You must make the source code of any distributed version (original or modified) available under the same GPLv3 license;
- You must retain proper attribution to the original author;
- You may not impose additional restrictions, nor use the code in any proprietary or closed-source form;
- Any derivative work must also be licensed under GPLv3.

Important context:  
This project was originally developed under the MIT License during its private development stage, but it was never publicly released under that license. As the project has not been previously published, the current and all future public releases are fully licensed under GPLv3. All previous commits are retroactively re-licensed under GPLv3.

I take this licensing choice seriously to protect the project’s integrity and to prevent abuse through repackaging, commercialization, or the addition of advertisements behind paywalls.

Please respect this license when using or distributing this project. Forks, modified versions, or redistributed binaries must also comply with GPLv3 and be released with full source code.

Note on third-party code:  
This project includes components such as `terminal-view` and `terminal-emulator`, derived from the [Termux project](https://github.com/termux/termux-app), which are distributed under the Apache License 2.0. This license is GPLv3-compatible, and their inclusion complies with the terms of both licenses.

I appreciate contributions from the open-source community and welcome any questions or discussions regarding licensing via GitHub Issues.

## Used Projects
[ReTerminal by RohitKushvaha01 - MIT License](https://github.com/RohitKushvaha01/ReTerminal)

## Important Notice
**This project is completely free.** If you **paid** for this software, **request a refund immediately.**

Download the app for **free** from the Releases page.

## FAQ
1. Why so many auto-commits?

This is a result of using b.sh. Rest assured, future commits will have proper messages.

2. Why do some committers lack GitHub avatars?

This was due to a git configuration issue and will not happen again.

3. I want a new feature!

Please open an Issue to request features. I'll review them.
