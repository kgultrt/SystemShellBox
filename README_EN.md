[查看此文档的中文版本](README.md)

# System Shell Box

## What is this?

**System Shell Box (SSB)** is a free and open-source file management tool inspired by MT Manager, with core functionalities fully implemented via terminal emulator.
📦 The purpose of this project is to provide users with a **lightweight, scalable** alternative to the paid functionality of MT Manager without relying on closed-source functionality.

## How to Build?

### Steps
1. Clone the repository:
   ```bash
   git clone https://github.com/
   ```

2. Modify Gradle configuration:  
   Open `gradle.properties` in the project root directory and **remove the following line**:
   ```properties
   # Environment-specific settings (delete this line)
   android.aapt2FromMavenOverride=/data/data/com.termux/files/home/.androidide/aapt2
   ```

3. Build with Android Studio or command line:
   ```bash
   ./gradlew assembleDebug
   ```

## How to Contribute?

⚠️ **Important**: This repository **does not accept external Pull Requests** currently. If you need custom modifications, please fork the project and build your own branch.

☕ Buy me a cup of coffee: not necessary, but thank you so much for donating support.

## License
This project is licensed under the **[MIT License](LICENSE.txt)**. You are free to use, modify, and distribute the code.

## Remind of something
**This project is completely free of charge.** If you **pay** for this software, **please refund.**

And you can download the app for **free** from the Release page.