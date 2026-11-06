# 发布与维护

仓库历史已压缩为单一提交：**tag 是版本唯一事实来源**，不要依赖旧提交 SHA 或旧 `v0.2.0`。

## 分支模型

- `main`：唯一开发线，承载最新版本；所有功能、修复先落到这里。
- `release/<major>.<minor>`：只在需要长期维护旧版本时创建；`main` 不合并进 `release/*`。
- `feature/*`、`hotfix/*`：短期分支，合并后删除。
- 多 Minecraft 版本不建分支，用 `adapter/<mc版本>/` 子工程表达。

## 版本号

`gradle.properties` 的 `mod_version` 是 mod 版本；`mods.toml` 与 jar 名自动跟随。Minecraft / Forge / mappings 版本在 `adapter/<版本>/gradle.properties`。

- **patch**：修 bug，不改配置格式与行为契约。
- **minor**：新功能、新界面、新 adapter。
- **major**：配置格式或行为不兼容。

## 发布步骤

1. 在 `main` 上确认测试与目标 adapter 构建通过：
   ```powershell
   .\gradlew.bat :common:test :adapter:1_18_2:build --no-daemon
   ```
2. 修改 `gradle.properties` 的 `mod_version`，同步 README / 游戏内说明。
3. 提交：`chore(release): <version>`。
4. 打 annotated tag：
   ```powershell
   git tag -a vX.Y.Z -m "XMAutoTool X.Y.Z"
   git push origin main
   git push origin vX.Y.Z
   ```
5. GitHub Actions 的 `release` 工作流在 tag 上构建所有 adapter，并把 jar 附加到 GitHub Release。
6. 检查 Release 资产：`xmautotool-<mc版本>-<mod版本>.jar`。

## 热修

- 从对应 tag 切 `hotfix/<x.y.z>`（若有维护线，则从 `release/<major>.<minor>` 切）。
- 修复、补测试，bump patch，重复发布步骤。
- 仅当 `main` 也受影响时，把修复 cherry-pick 到 `main`。
- 禁止 rebase / force-push 已发布的 `main`（历史重写属于仓库级操作，需单独评审并通知所有使用者）。

## 新增 Minecraft 版本

1. 复制 `adapter/1_18_2/` 为 `adapter/<新版本>/`。
2. 改该目录的 `gradle.properties`（Minecraft/Forge/mappings）与 `build.gradle`。
3. 实现同一组端口：`Host`、`Game`、`Canvas`；界面与引擎代码在 `common`，不改。
4. `settings.gradle` 加一行 `include 'adapter:<新版本>'`。
5. 在 `.github/workflows/release.yml` 的 matrix 里加该版本。
6. 本地验证：`.\gradlew.bat :adapter:<新版本>:build --no-daemon`。

## CI 说明

`.github/workflows/release.yml`：

- 触发：推送 `v*` tag。
- 矩阵：每个 adapter 一个构建 job；先跑 `:common:test`，再构建该 adapter。
- 发布 job 汇总所有 jar，创建/更新 GitHub Release（`contents: write`）。
- 首次真实运行以 Actions 日志为准；本地只校验 YAML 语法。

## 注意事项

- 发布前确认 `git status` 干净、`mod_version` 与 tag 一致。
- `docs/` 不入库（本地台账）；`AGENTS.md` 入库。
- 历史压缩后旧 tag/分支已删除；不要凭旧 SHA 取文件。
