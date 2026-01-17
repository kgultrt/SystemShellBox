# 贡献指南 | Contribution Guide

## 欢迎！ | Welcome!

感谢您对本项目的关注！我们欢迎任何形式的贡献。在开始之前，请务必阅读以下指南。

Thank you for your interest in this project! We welcome contributions of all kinds. Before you start, please read this guide carefully.

---

## 语言要求 | Language Requirements

### 重要限制 | Important Restrictions
1. **不接受 Kotlin 源代码**：本项目不允许提交任何 Kotlin (`.kt`) 源代码文件。提交的代码中不得包含 Kotlin 语法。
   **No Kotlin Source Code**: This project does NOT accept any Kotlin (`.kt`) source files. Submitted code must not contain Kotlin syntax.
2. **允许的 Kotlin 库**：仅允许使用提供 **Java API** 的 Kotlin 库（通过 Java 能直接调用）。
   **Allowed Kotlin Libraries**: Only Kotlin libraries that provide a **Java API** (directly callable from Java) are allowed.
3. **其他语言**：可以使用 Java、C/C++（通过 JNI）等。请勿引入复杂工具链（如需要特殊构建环境）。
   **Other Languages**: Java, C/C++ (via JNI), etc. are acceptable. Please avoid introducing complex toolchains.

### 为什么？ | Why?
- 项目维护者基于过往经验，决定在项目中不使用 Kotlin 语言，以保持代码一致性和维护简便性。
- 项目保持简单的技术栈，便于维护。
- The maintainer has decided not to use Kotlin in the project based on past experience, to maintain code consistency and ease of maintenance.
- The project aims to keep a simple tech stack for easier maintenance.

---

## 如何贡献？ | How to Contribute?

### 1. 报告问题 | Reporting Issues
- 在提交问题前，请先搜索是否已有类似问题。
- 描述问题请包括：Android 版本、设备型号、复现步骤、期望结果与实际结果。
- Before submitting an issue, search to see if it already exists.
- Include: Android version, device model, steps to reproduce, expected vs actual results.

### 2. 提交功能请求 | Submitting Feature Requests
- 清楚描述功能的使用场景和预期行为。
- 讨论可能的设计方案或实现思路。
- Clearly describe the use case and expected behavior.
- Discuss possible design or implementation approaches.

### 3. 提交代码更改 | Submitting Code Changes
- 请先开一个讨论或问题描述你的想法。
- 确保代码符合项目现有的编码风格。
- 保持更改的简洁和专注（一次只解决一个问题）。
- Start by opening a discussion or issue to describe your idea.
- Ensure code follows the existing coding style of the project.
- Keep changes concise and focused (one issue per change).

---

## 贡献流程 | Contribution Process

1. **Fork 仓库** Fork the repository
2. **创建 Pull Request** Open a Pull Request

---

## 许可证 | License

所有贡献将按项目现有许可证（查看 `LICENSE` 文件）授权。

All contributions will be licensed under the project's existing license (see `LICENSE` file).

---

## 问题？ | Questions?

如果您有任何疑问，请通过 Issues 页面提出。

If you have any questions, please open an issue on the Issues page.
