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
2. Open keystore.properties.sample, modify its contents, configure your signature file, and change the file name to keystore.properties

3. Build using Android Studio or command line (do NOT use b.sh):
   ```bash
   ./gradlew assembleDebug
   ```

## How to Contribute?

If you find a bug, you can create a PR/open an Issets
Thank you very much for your contribution.

Give me the money: Not required, but greatly appreciated if you'd like to support the project. (Donation methods not yet available)

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

I appreciate contributions from the open-source community and welcome any questions or discussions regarding licensing via GitHub Issues.

## Used Projects
[termux-app - GPLv3 and Apache 2.0](https://github.com/termux/termux-app)

[ApkSignatureKillerEx - No LICENSE](https://github.com/L-JINBIN/ApkSignatureKillerEx)

[CodeEditor - GPLv3](https://github.com/MrIkso/CodeEditor)

Because the way projects are used is a bit special, let's clarify where the code for these projects is:

termux-app:

app/src/main/java/com/termux/app/\* (GPLv3)

terminal-view/\* (Apache 2.0)

terminal-emulator/\* (Apache 2.0)

ApkSignatureKillerEx:

app/src/main/jni/signature/\* (No LICENSE)

app/src/main/java/com/manager/ssb/util/SignatureVerify.java (No LICENSE)

CodeEditor:

app/src/main/java/com/mrikso/codeeditor/\* (GPLv3)

Some changes have been made to the code during this period for the sake of adaptability. Please check it yourself.

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
