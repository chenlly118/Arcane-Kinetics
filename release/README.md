# 维护者发布流程

当前发布目标：**Arcane Kinetics 0.5.0 Beta**，Minecraft 1.21.1 / NeoForge / Java 21。发行资料见 [project.json](project.json)，双语说明见 [notes/0.5.0.md](notes/0.5.0.md)。[0.3.0 首发](https://github.com/chenlly118/Arcane-Kinetics/releases/tag/v0.3.0)保留原样。

1. 更新 `gradle.properties`、`project.json`、`approvals.json` 和对应的 `notes/<版本>.md`。得到作者明确批准后，记录本版公开发布授权。检查提交内容，排除缓存、原型、本地截图和测试存档。
2. 提交到 `main`，等待 **Build candidate** 成功。构建流程只读取仓库，使用 Java 21 和 Gradle，扫描源码并验证 JAR，将 `release-candidate` 保留 30 天。审核其提交号和 `validation.json` 中的 JAR SHA-256。
3. 在 `main` 手动运行 **Publish approved candidate**，输入 tag（如 `v0.5.0`）、候选 CI run ID、64 位小写 SHA-256 及 `publish-` 加 tag（如 `publish-v0.5.0`）。也可在审核过的 CI 提交创建 `codex/publish/<tag>/<run_id>/<sha256>`。普通 `main` 提交只触发构建。
4. 发布流程下载并核对原 CI 产物，使用 [MC-Publish v3.3.2](https://github.com/Kira-NT/mc-publish/releases/tag/v3.3.2) 向 GitHub Release、Modrinth 和 CurseForge 上传同一 JAR；GitHub 另附 `SHA256SUMS.txt`。工作流回读 GitHub 和 Modrinth 文件哈希，并记录 CurseForge 文件 ID。CurseForge 文件公开后，可下载并用同一 SHA-256 校验。

仓库已配置 `MODRINTH_TOKEN` 和 `CURSEFORGE_TOKEN` Actions Secrets。Modrinth Slug 为 `create-spellbook`，工作流会验证其项目 ID；CurseForge 项目 ID 为 `1773802`。GitHub 由 Actions 提供 `GITHUB_TOKEN`。Token 只从 Secrets 注入。

三个平台的上传各自独立。部分失败时，先核对已生成的版本、文件 ID 和哈希，再用原候选 JAR 补发缺失的平台，避免重复上传。`curseforge-existing.yml` 仅用于已完成的 0.3.0 补发，不用于新版本。
