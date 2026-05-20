# Dialogue Extractor (Android)

上传包含 AI 对话 JSON 的 `.txt` / `.json` 文件，提取“用户”和“AI”正文并导出为可读 `.txt`。

## 功能
- 过滤 `isThought=true` 或 `thought=true` 的块与子块（不提取思维链）。
- 仅保留 `role` 为 `user` 或其他助手角色的正文文本。
- 自动将 `\n` 转为真实换行，输出更易读。

## 使用
1. 打开 App，点击 **选择输入文件**。
2. 选择你的聊天记录 `.txt`（内容需是 JSON）。
3. 点击 **提取并保存 txt**，选择输出路径。

## GitHub Actions 打包与 Release
- 工作流文件：`.github/workflows/android-release.yml`
- 触发方式：
  - 推送标签（例如 `v1.0.0`）自动构建并发布 APK 到 Release。
  - 手动触发（workflow_dispatch）会自动生成标签并发布 APK。
