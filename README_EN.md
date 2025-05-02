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
This project is licensed under the **[MIT License](LICENSE.txt)**. You are free to use, modify, and distribute the code.

The `terminal-view` and `terminal-emulator` components are sourced from [Termux](https://github.com/termux/termux-app) and are distributed under the [Apache 2.0 License](https://www.apache.org/licenses/LICENSE-2.0), which is compatible with MIT.

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
