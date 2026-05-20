# Dialogue Extractor (Android)

上传包含 AI 对话 JSON 文本的 `.txt` 文件，提取用户与 AI 的正文，去除思维链和无关元数据，导出为易读 `.txt`。

## 功能
- 选择输入 TXT（内容是 JSON 字符串）。
- 自动过滤 `isThought=true` / `thought=true` 的 chunk 和 part。
- 仅保留 `user` 与 AI 对话正文。
- 自动把字面量 `\\n` 转成真正换行。
- 一键导出 `extracted_dialogue.txt`。

## GitHub Actions
仓库内置 `.github/workflows/android-release.yml`：
- 手动触发 `workflow_dispatch` 构建 release APK。
- 推送 `v*` 标签时自动构建并创建 GitHub Release，附带 `app-release.apk`。
