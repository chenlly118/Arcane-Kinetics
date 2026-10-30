# 维护者发布流程

当前版本 **0.3.0 / Beta**。作者已明确确认 MIT、素材分发权和公开发布，已发布 0.3.0 的授权记录见历史提交；main 的更名与图标修改仅更新页面和源码，`public_release_approved=false`，下次发布前需更新版本并明确批准；授权依据见 `docs/LICENSING_REVIEW.md`。2026-10-30 已发布 [GitHub 0.3.0 Beta](https://github.com/chenlly118/Arcane-Kinetics/releases/tag/v0.3.0)，Modrinth 文件已上传并提交审核（processing）。[构建记录](https://github.com/chenlly118/Arcane-Kinetics/actions/runs/41017841524) · [发布与哈希校验](https://github.com/chenlly118/Arcane-Kinetics/actions/runs/41017949710)。

## 首次账号准备

1. 用户已创建 [chenlly118/Arcane-Kinetics](https://github.com/chenlly118/Arcane-Kinetics)，现为 **Public** 开源仓库；不需要再创建或重命名。现有连接可以读取和写入该仓库，但不提供设置 Secrets 的接口。
2. 本地 main 已有源码提交，origin 已指向该仓库。推送前用 `git status`、`git log` 与 `git ls-files` 审阅范围；不要使用 `git add -f` 把缓存／素材参考强行加入。
3. 推送经过扫描的源码并运行只构建的 CI；这一步不上传正式 Release 或 Modrinth 文件。用户已批准将仓库公开。
4. 用户已创建 [Modrinth 项目](https://modrinth.com/project/create-spellbook/settings)。使用已确认的 Slug `create-spellbook`；MC-Publish v3.3 支持 Slug，工作流会用 Secret 认证读取真实 ID。资料见 `project.json` 和 `modrinth-description.md`，许可证选择 MIT。
5. 默认直接使用 Slug，无需配置项目 ID Variable；`modrinth_project_id` 可为空。需要固定正式 ID 时可填写该字段并设置 GitHub Variable **MODRINTH_PROJECT_ID**。
6. 在 Modrinth 账号设置生成仅含发布版本所需权限的 Token，**由用户直接存入 GitHub Actions Secret `MODRINTH_TOKEN`**。不要粘贴到聊天、源码、命令参数或日志。新项目同步资料及提交审核需要 Read/Write projects，上传与固定依赖版本需要 Create/Write versions；无需创建项目或删除权限。GitHub 使用自动提供的 `GITHUB_TOKEN`。
7. 发布工作流使用 **release** environment；可按维护者需要配置审核规则。用户已确认仓库级 `MODRINTH_TOKEN` Secret 配置完成，工作流可直接使用；无需复制或读取其值。

## 构建与审核

`build.yml` 在 main push、PR 和手动触发时构建，只有读仓库权限，不包含发布 Token。它执行源码扫描、Gradle build 和 JAR 验证，将一个 `release-candidate` Actions artifact 保留 30 天。该 artifact 为 CI 候选产物，不是正式 Release。

审核本轮所用 commit、候选 JAR、`validation.json`、`SHA256SUMS.txt` 和版本说明。使用已记录授权的 commit 在 main push 生成最终候选，核对最终 run 的 JAR SHA-256。发布前追加的鼓风熔炼三倍粒子调整必须包含在该 commit 中。

本地构建命令：

```sh
./gradlew --no-daemon build
python scripts/package_public_release.py --jar build/libs/create-spellbook-1.21.1-neoforge-0.3.0.jar --build-method gradle
```

输出到 `build/release-candidate/0.3.0/`。本地安装环境 fallback 构建仅能标记 `installed-runtime`；发布 workflow 拒绝这种构建方法，必须使用成功的标准 Gradle CI 产物。

## 获批准后发布

1. 确认最终 main push CI 成功，并审核其 SHA-256；发布工作流会在该 CI commit 建立 `v0.3.0` 标签，不覆盖现有 Release。
2. 用户批准公开后，维护者将 GitHub 仓库切换为 Public。Modrinth 项目资料与公开审核也需要用户批准；平台人工审核是否通过不能由 workflow 保证。
3. 从 main 手动运行 **Publish approved candidate**，输入 tag `v0.3.0`、最终 CI run ID、64 位小写 SHA-256 和 `publish-v0.3.0`。也可在同一 CI commit 创建 `codex/publish/v0.3.0/<run_id>/<sha256>` 分支作为明确发布请求；普通 main 提交只构建，不发布。
4. workflow 检查 CI 来源必须是 `build.yml` 的成功 main push，且 commit 与审核来源一致；下载原 artifact，检查许可／素材／公开批准、项目 ID 和审核哈希。它不会重新编译。上传前用 Slug 认证读取项目，校验目标并同步 MIT、介绍、分类和源码链接；缺少权限会在上传文件前停止。
5. [MC-Publish v3.3](https://github.com/Kira-NT/mc-publish/tree/v3.3) 同时发布 GitHub prerelease 与 Modrinth beta，只上传确定的主 JAR；GitHub 额外附带 SHA256SUMS。随后从 GitHub 下载文件逐字节比对，并读取 Modrinth SHA-512／SHA-1 校验。MC-Publish v3.3 的依赖为项目级引用，辅助脚本使用官方 API 补齐审核过的具体依赖版本；最后提交新项目公开审核。
6. 保存两平台实际 URL 和 Actions run 链接，确认依赖、游戏版本、加载器及 Beta 标签。更新文档“待发布”状态。

## 失败与下一版

双平台不是原子事务。部分成功时，先查看两平台的版本与哈希；不要直接重跑整个发布动作、重复上传或覆盖现有文件。只补发缺失平台，继续使用审核过的同一 JAR。发现内容错误应发布新版本，不替换相同版本的文件。

后续版本修改 `gradle.properties`、`project.json`、审批版本、README 和对应 notes，重新将三项审批置 false。CI 自动按 Gradle 版本打包；本首发 workflow 固定依赖和 beta 类型，变更依赖或发布稳定版时同步修改工作流及验证器。

此流程已通过远程构建、双平台上传和哈希回读验证；平台审核状态与验证结果见 `docs/PRE_RELEASE_REVIEW.md`。
