# XMAutoTool

Minecraft Forge 的纯客户端自动选工具模组。核心逻辑、配置与界面在纯 JVM 的 `common`，Minecraft/Forge 依赖只在 `adapter/<MC版本>`；当前提供 **1.18.2 + Forge 40.3.11** 适配，新增 MC 版本只需补 adapter。

模型：一条「策略」= 左边圈方块 + 右边圈工具 + 一个动作，
按顺序首个命中生效，未命中走兜底（按实测 harvest/speed 选）。

## 已实现

- **配置文件（Profile）**：所有用户配置都是文件，可保存多份并随时切换；一个文件包含
  `settings / policies / toolOverrides / kinds / blacklists`。切换立即加载，编辑自动保存。
- **策略引擎**：`USE / NEVER / FORCE`、右圈权重、正确性门槛三态、速度迟滞、平手规则、黑名单、耐久过滤。
- **圈定**：方块 `id / tag / mod / states`；工具 `slot / nameContains / fingerprint / item / type / capability / nbt / any`。
- **能力解析**：Forge `ToolActions` → 原版类兜底 → `item → kinds` 覆盖（可带 `assumeCorrect` / `speedOverride`）。
- **界面**：
  - 主界面：左侧策略列表 + 操作区；右侧配置文件、全部行为设置（开关 + 数值）与工具/说明入口。
  - 策略编辑器：左右并排，方块在左、工具在右，带图标滚动列表，id 前缀补全；
    方块支持「目视 / 手持 / 输入」，工具支持「手持 / 背包 / 自动」。
  - 配置文件页：新建、复制、重命名、删除、切换。
  - 工具识别页：给识别不到的工具指定类型；工具类型页：新增/改名锤、镰等类型，父类型 chip 带图标。
  - 说明页：左侧章节 + 右侧分页文本，解释策略、匹配器、类型、识别、指纹等全部概念。
- **交互细节**：按钮统一为扁平样式；输入框与按钮同高；右上角图标关闭/返回。
- **展示引导**：开启后在按钮/输入框悬停显示说明（默认开，可在主界面关闭）。
- **触发与保护**：左键、挖掘中按间隔评估；侧守卫、开界面/旁观/使用物品/睡眠/抑制键时不切；切槽冷却。
- **缓存**：方块快照按 `policyHash` 缓存；指纹/NBT 只在策略用到时计算。

## 文件位置

```
config/xmautotool/profiles/<id>.json   # 全部用户配置（每个文件一份完整配置）
config/xmautotool-client.toml          # 只保留 activeProfile（当前文件 id）与 debug
```

- 旧版 `config/xmautotool/policies.json` 首次启动会自动迁移到 `profiles/default.json`。
- 不再使用「全局 / 按存档」作用域，也不再写 `saves/<world>/...`；不同存档/服务器直接切配置文件即可。

## 工程结构

同一个 git 仓库、Gradle 多项目：

- `common/`：纯 JVM。`core` 策略引擎、`config` 配置读写、`gui` 全部界面/控件/布局、`api` 端口、`trigger` 守卫状态机。**不依赖 Minecraft/Forge**。
- `adapter/1_18_2/`：唯一应用 ForgeGradle、持有 MDK 的适配层。实现 `Host` / `Game` / `Canvas` 三个端口，含 mod 入口、Forge 配置/按键/事件桥、方块物品能力与注册表访问、资源。
- 加新版本：新建 `adapter/<版本>/` 实现同一组端口，在 `settings.gradle` include 一行；`common` 不动。

## 构建

要求 JDK 17。仓库内不含 `gradle` 发行版，首次构建需要通过代理下载依赖。

```powershell
$env:JAVA_HOME = 'D:\Tool\Java\jdk-17.0.4+8'
$env:GRADLE_USER_HOME = "$PWD\.gradle-home"
$env:GRADLE_OPTS = '-Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7890 -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7890'
.\gradlew.bat build --no-daemon                 # 默认 adapter/1_18_2
.\gradlew.bat build -Padapter=1_18_2 --no-daemon
.\gradlew.bat :adapter:1_18_2:build --no-daemon # 等价，直接指定子工程
```

产物：`adapter/1_18_2/build/libs/xmautotool-1.18.2-<version>.jar`，是 common + adapter 合并后的单个 mod jar。

- Gradle 与依赖缓存在仓库内 `.gradle-home/`（已 gitignore），不会污染用户目录。
- 代理属于机器本地设置，放在 `.gradle-home/gradle.properties`，不写进构建脚本。
- 受限会话下 JDK 的 zipfs 会把所有文件判成只读（`Files.isWritable` 恒 false），
  ForgeGradle 的 access transformer / reobf 会失败；这类会话需要完全权限执行构建。

## 测试

```powershell
.\gradlew.bat :common:test --no-daemon
```

`common` 整个模块不依赖任何 Minecraft 类型，`PolicyEngineTest` 全部是纯 JVM 用例。

## 游戏内

1. 把 jar 放进 `mods/`，启动游戏。
2. 按 `G` 打开主界面；右侧可直接改全部设置，「配置文件」按钮切换/新建/复制；右上角图标关闭。
3. 左侧「新建」一条策略 → 左栏加方块（目视/手持/输入），右栏加工具（手持/背包/自动）→ 返回即保存。
4. 左键挖方块即生效；没配策略的方块走兜底。
5. 识别不到的工具去「工具识别」；需要新类型（锤/镰）去「工具类型」；不懂的概念看「说明」。

## 已知边界

- 引导 tooltip 是单行文本，超长会截断到屏幕内。
- `nbt` 匹配器、`weight` 权重、方块/工具黑名单目前只有 JSON 入口。
- 工具类型可以新增/删除/改名，但内置类型的父类型不能在界面里改（JSON 里可以）。
- 当前 adapter 为 1.18.2 + Forge 40.x；其他 MC 版本按 `adapter/<版本>/` 补充，`common` 不改。不针对任何整合包写专用代码。

## 文档与仓库

- `AGENTS.md`：协作规则，纳入版本控制。
- `RELEASING.md`：发布规则。tag 形如 `v0.3.0-1.18.2`，每个 MC 版本一个 release。
- `docs/`（含 UI 迭代台账与会话计划）仅本地保留，已在 `.gitignore` 中忽略。
- 构建产物（`build/`）、Gradle 缓存（`.gradle/`、`.gradle-home/`）、日志与临时文件同样不纳入仓库。
- 设计说明已并入游戏内「说明」页与 README；不再保留独立设计稿。
