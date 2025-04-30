[View the English version of this document](README_EN.md)

# System Shell Box

## 这是什么？

**System Shell Box (SSB)** 是一个免费开源的文件管理工具，设计灵感来源于MT管理器，但完全基于终端模拟器实现核心功能。  
📦 本项目旨在为用户提供一个**轻量、可扩展**的MT管理器付费功能替代解决方案，无需依赖闭源功能。

## 如何构建？

### 步骤
1. 克隆仓库：
   ```bash
   git clone https://github.com/
   ```

2. 修改 Gradle 配置：  
   打开项目根目录下的 `gradle.properties` 文件，**删除以下行**：
   ```properties
   # 专门环境设置，请删除这一行
   android.aapt2FromMavenOverride=/data/data/com.termux/files/home/.androidide/aapt2
   ```

3. 使用 Android Studio 或命令行构建 (请不要使用b.sh)：
   ```bash
   ./gradlew assembleDebug
   ```

## 如何贡献？

⚠️ **重要**：本仓库**暂不接受外部 Pull Request**。若你需要自定义修改，请直接下载源码并自行构建分支。

☕ 请我喝杯咖啡: 不是必要的，但非常感谢你能捐赠支持。

## 开源协议
本项目采用 **[MIT 许可证](LICENSE.txt)**，您可以自由使用、修改和分发代码。

其中，terminal-view 还有 terminal-emulator 来自于 https://github.com/termux/termux-app 它们是在 [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) 下进行分发，与 MIT 兼容

## 提醒
**本项目完全免费，**若您是**付费**购买的此软件，**请退款。**

并且从发行版页面**免费**下载此应用。
