[View the English version of this document](README_EN.md)

# System Shell Box

## 这是什么？

**System Shell Box (SSB)** 是一个免费开源的文件管理工具，设计灵感来源于MT管理器，但完全基于终端模拟器实现核心功能。  
📦 本项目旨在为用户提供一个**轻量、可扩展**的MT管理器付费功能替代解决方案，无需依赖闭源功能。
在此声明: 本应用立志于成为一个开发工具，反编译也许会支持，但不会太多

**注意**: 此应用目前还处于早期，功能相当不完善

🎓 低维护状态警告：目前维护者马上快高中住校了，预计更新频率较低（约每学期1-2次）。但只要抽空，一定会积极维护，感谢您的理解与耐心！

## 项目演示

![项目演示](./demo.gif)

## 如何构建？

### 步骤
1. 克隆仓库：
   ```bash
   git clone https://github.com/kgultrt/SystemShellBox
   ```

2. 打开 keystore.properties.sample，修改其内容，并配置你的签名文件，并把文件名更改为keystore.properties

3. 使用 Android Studio 或命令行构建：
   ```bash
   ./gradlew assembleDebug
   ```

## 如何贡献？

若您发现了一个bug，您可以创建一个PR/开启一个Issets

十分感谢你的贡献

打钱: 不是必要的，但非常感谢你能捐赠支持。 (捐赠方式暂时不提供)

## 开源协议

本项目自首次公开发布起，采用 [GNU 通用公共许可证第 3 版](LICENSE.txt)（GPLv3）进行授权。

您可以自由使用、修改和分发本项目的全部或部分代码，但必须遵守以下条件：

- 必须在分发时同时提供源代码；
- 必须保留原作者的署名与本许可证文本；
- 所有基于本项目的修改版本，亦必须在相同许可证下发布（即必须同样采用 GPLv3）；
- 不得将本项目或其衍生版本闭源、商业化或以任何形式添加专有条款进行再分发。

本项目原始开发阶段采用 MIT 协议进行标注，但在正式公开源代码前，项目始终处于私有状态，并**未以 MIT 协议形式发布过任何版本**。因此，自首个公开版本起，整个代码库（包括历史提交）视为统一在 GPLv3 协议下授权，并不再适用于 MIT 协议。

特别说明：

- 任何从本项目 Fork 的代码、发布的衍生应用或修改版本，必须同样在 GPLv3 下公开其完整源代码；
- 本项目明确反对将其用于魔改、闭源、插入广告或付费墙的再发布行为；
- 本项目的发布目的是服务开发者社群，不欢迎任何企图将其转为牟利产品的行为。

我感谢开源社区的贡献，并希望您在遵守协议的前提下，充分使用并贡献本项目。如果您有任何有关许可证的问题，欢迎通过 Issues 联系我进行探讨。

## 使用的项目
[termux-app - GPLv3 and Apache 2.0](https://github.com/termux/termux-app)

[ApkSignatureKillerEx - No LICENSE](https://github.com/L-JINBIN/ApkSignatureKillerEx)

[CodeEditor - GPLv3](https://github.com/MrIkso/CodeEditor)

因为使用项目的方式有点特殊，在此明确一下这些项目的代码到底在哪里:

termux-app:

app/src/main/java/com/termux/app/\* (GPLv3)

terminal-view/\* (Apache 2.0)

terminal-emulator/\* (Apache 2.0)


ApkSignatureKillerEx:

app/src/main/jni/signature/\* (No LICENSE)

app/src/main/java/com/manager/ssb/util/SignatureVerify.java (No LICENSE)


CodeEditor:

app/src/main/java/com/mrikso/codeeditor/\* (GPLv3)


期间为了适配性对代码做了些许的更改，请自行查阅。

## 提醒
**本项目完全免费，**若您是**付费**购买的此软件，**请退款。**

并且从发行版页面**免费**下载此应用。

## 版本号管理

项目使用本地构建号文件 (`buildNumber.txt`) 来跟踪调试版本的构建次数。

此文件不会提交到版本控制，每个开发者有自己的构建计数。

## 常见问题
1. 为什么这么多 auto commit?

这是我使用 b.sh 的结果，请放心，项目之后就会有详细的提交信息了，不会这样

2. 为什么提交人github没有头像？

这是我的 git 配置问题，之后不会再有了

3. 我想要新功能！

请在 Issues 提交一个问题，我会看到的