# Mihon Android 项目规则

> 本文件是 Claude Code 的项目规则，描述了 Mihon 项目的架构、规范和开发约定。

---

## 一、项目概览

Mihon 是一个开源 Android 漫画阅读器，从 Tachiyomi 分叉而来。支持通过可扩展的图源插件系统浏览和阅读漫画，内置书架管理、下载、阅读历史追踪等功能。

- **语言**：Kotlin（Kotlin Multiplatform，commonMain + androidMain）
- **最低 SDK**：Android 8.0 (API 26)
- **构建系统**：Gradle（Kotlin DSL），使用 build-logic 约定插件
- **UI 框架**：Jetpack Compose + Material 3

---

## 二、模块架构

```
mihon/
├── app/              # 主应用模块（UI 层 + DI 注入）
├── core/             # 通用核心工具（coroutines 扩展、日志、存储工具等）
├── core-metadata/    # 漫画元数据解析（ComicInfo.xml 等）
├── data/             # 数据访问层（SQLDelight 实现、Repository 实现）
├── domain/           # 领域层（实体、用例/Interactor、Repository 接口）
├── i18n/             # 国际化（moko-resources）
├── presentation-core/# 共享 Compose 组件
├── presentation-widget/# App Widget
├── source-api/       # 图源 API 接口定义
├── source-local/     # 本地图源实现（KMP：commonMain + androidMain）
├── macrobenchmark/   # 性能基准测试
└── telemetry/        # 遥测
```

### 依赖方向（铁律）

```
app/ → 所有模块
presentation-core/ → domain/, core/, i18n/
data/ → domain/, core/, source-api/
domain/ → core/
source-local/ → source-api/, core/, domain/
core/ → 无业务依赖
```

- `domain/` 定义 Repository 接口，`data/` 提供实现
- `source-api/` 定义图源接口，`source-local/` 实现本地图源
- 模块间不可循环依赖

---

## 三、技术栈与关键框架

| 技术 | 用途 | 注意事项 |
|------|------|----------|
| **Jetpack Compose** | 全部 UI | Material 3，使用 `Scaffold` 来自 presentation-core |
| **Voyager** | 导航 + ScreenModel | `StateScreenModel<State>`, `rememberScreenModel` |
| **SQLDelight** | 数据库 | `.sq` 文件定义查询，自动生成类型安全 API |
| **Paging 3** | 远程图源分页 | `Pager` + `LazyPagingItems`；本地图源不用 Paging |
| **moko-resources** | 国际化 | `MR.strings.xxx`，`stringResource()` |
| **Injekt (Kodein)** | 依赖注入 | `Injekt.get()`, 构造函数注入优先 |
| **kotlinx.coroutines** | 异步 | `screenModelScope`, `launchIO`, `launchNonCancellable` |
| **kotlinx.serialization** | JSON 序列化 | |
| **UniFile (SAF)** | 文件操作 | 本地图源文件系统访问 |
| **Coil** | 图片加载 | |

### KMP expect/actual

`source-local` 模块使用 Kotlin Multiplatform：
- `commonMain`：定义 `expect class`（如 `LocalSource`, `LocalSourceFileSystem`, `LocalCoverManager`）
- `androidMain`：提供 `actual class` 实现
- 新增 expect 声明时，**必须同时提供 actual 实现**

---

## 四、架构模式

### 分层架构

```
UI (Compose Screen/Content)
  ↓ 调用
ScreenModel (Voyager StateScreenModel)
  ↓ 调用
Interactor / UseCase (domain/)
  ↓ 调用
Repository 接口 (domain/) ← Repository 实现 (data/)
  ↓ 调用
SQLDelight / 网络 / 文件系统
```

### ScreenModel 规范

- 继承 `StateScreenModel<State>`，State 标注 `@Immutable`
- State 是 `data class`，不可变，通过 `mutableState.update { it.copy(...) }` 更新
- 副作用用 `screenModelScope.launchIO {}` 或 `launchNonCancellable {}`
- 对话框状态放在 `State.dialog`，类型为 `sealed interface Dialog`
- 业务逻辑不直接写在 Compose 中

### Compose Screen 规范

- Screen 类实现 `Screen()` 接口
- `@Composable Content()` 内通过 `rememberScreenModel` 获取 ScreenModel
- `state by screenModel.state.collectAsState()` 订阅状态
- 事件回调向下传递（`onXxxClick`），不在子组件内处理导航
- 使用 presentation-core 的 `Scaffold`（不是 material3 的）

### Repository 模式

- `domain/` 定义接口（`interface XxxRepository`）
- `data/` 实现接口（`class XxxRepositoryImpl`）
- 通过 `DomainModule.kt` 注册 DI 绑定
- Interactor 封装单一用例，命名为 `GetXxx` / `SetXxx` / `DeleteXxx`

---

## 五、关键目录导航

### app/ 主要结构

```
app/src/main/java/eu/kanade/
├── presentation/       # Compose UI 组件
│   ├── browse/         # 图源浏览（含 BrowseSourceScreen/Content）
│   ├── library/        # 书架
│   ├── manga/          # 漫画详情
│   ├── reader/         # 阅读器
│   └── components/     # 共享 UI 组件
├── tachiyomi/
│   ├── ui/             # Screen + ScreenModel
│   │   ├── browse/     # 图源浏览逻辑
│   │   ├── library/    # 书架逻辑
│   │   ├── manga/      # 漫画详情逻辑
│   │   └── reader/     # 阅读器逻辑
│   ├── data/           # Android 特有数据层（DownloadManager, CoverCache 等）
│   ├── di/             # DI 注入模块（AppModule.kt）
│   └── source/         # 图源管理
└── domain/             # App 层 DI 绑定（DomainModule.kt）
```

### 常用文件速查

| 需求 | 文件 |
|------|------|
| 添加 DI 绑定 | `app/.../domain/DomainModule.kt` + `app/.../di/AppModule.kt` |
| 添加数据库查询 | `data/src/main/sqldelight/tachiyomi/data/*.sq` |
| 添加国际化文案 | `i18n/src/commonMain/moko-resources/base/strings.xml` |
| 添加 Repository 方法 | `domain/.../repository/XxxRepository.kt` 接口 + `data/.../XxxRepositoryImpl.kt` 实现 |
| 修改本地图源 | `source-local/src/androidMain/.../LocalSource.kt` |
| 修改图源浏览 UI | `app/.../presentation/browse/` + `app/.../ui/browse/` |
| 修改书架 UI | `app/.../presentation/library/` + `app/.../ui/library/` |

---

## 六、编码规范

### Kotlin 风格

- 遵循 Kotlin 官方编码规范
- 优先使用不可变数据（`val`, `data class`, `copy()`）
- 集合操作优先使用 `kotlinx.collections.immutable`（`persistentListOf`, `toImmutableList`）
- 状态更新使用 `mutate {}` 扩展（来自 `mihon.core.common.utils`）

### Compose 风格

- Composable 函数首字母大写
- 回调参数命名 `onXxxClick` / `onXxxChange`
- 默认参数放最后
- `Modifier` 作为第一个可选参数
- 使用 `LaunchedEffect` 处理副作用，不在 Composition 中启动协程

### 并发

- `screenModelScope.launchIO {}` — 普通异步
- `screenModelScope.launchNonCancellable {}` — 不可取消的操作（如删除）
- `withIOContext {}` — 切换到 IO 调度器
- `async {} / awaitAll()` — 并行化独立操作
- `Mutex` — 防止同一操作并发执行（如 sync）
- `@Volatile` — 多线程可见性（配合简单缓存）
- `ConcurrentHashMap` — 线程安全缓存

### 数据库操作

- SQL 查询定义在 `.sq` 文件中，不要手写 SQL 字符串
- 添加新查询后需确认 `MangaMapper` 等映射器正确处理
- Repository 接口方法返回 `Flow<T>` 用于响应式订阅，`suspend fun` 用于一次性操作

---

## 七、构建与验证

### 编译命令

```bash
./gradlew :app:compileDebugKotlin    # 编译验证（最常用）
./gradlew assembleDebug               # 构建 Debug APK
```

**注意**：不存在 `compileStandardDebugKotlin` 任务。

### 编译前必须验证的改动

- 修改 `expect` / `actual` 声明
- 添加/修改 `.sq` 查询
- 修改 DI 注册
- 修改 import
- 跨模块结构性变更

### Gradle Lock 问题

如遇 Gradle lock 冲突（PID 占用），用 `./gradlew --stop` 停止守护进程，必要时删除 `.gradle/noVersion/buildLogic.lock`。

---

## 八、Git 与工作流

### 提交信息格式

参照项目已有风格：简洁英文描述，说明做了什么。

### 危险 Git 操作

以下操作需用户确认：
- `git reset --hard` / `git push --force` / `git branch -D`
- `git clean -fd` / `git checkout -- .`

### 分支

- `main` 为主分支
- 功能开发在 feature 分支上进行

---

## 九、多选与批量操作模式

项目中多处使用的多选模式：

| 概念 | 实现 |
|------|------|
| 选中状态 | `State.selection: Set<Long>`（存储 manga ID） |
| 选中模式判断 | `State.selectionMode get() = selection.isNotEmpty()` |
| 切换选中 | `mutate {}` 扩展操作 Set |
| 退出选中 | `BackHandler` 监听 + `clearSelection()` |
| 选中 UI | Toolbar 切换为 SelectionToolbar，底部显示 ActionMenu |
| 长按进入 | 首次长按触发 `toggleSelection` + haptic feedback |

### 本地图源特殊处理

- 本地图源使用 `List<Manga>` 直接渲染（非 `LazyPagingItems`）
- 数据来自 DB 响应式 Flow（`getLocalManga.subscribe(query)`）
- 同步通过 `LocalMangaSyncService.sync()` 将文件系统状态写入 DB
- 删除后需要 `invalidateCache()` + `sync()` 保证一致性
- 侧滑删除使用 `SwipeableActionsBox`（me.saket.swipe 库）

---

## 十、注意事项

### 法律合规

- **禁止**在代码中硬编码任何插件仓库 URL（如 keiyoushi 等）
- **禁止**内置任何默认图源或仓库地址
- 这是项目的设计决策，旨在规避版权风险

### 性能

- 本地图源使用目录缓存（30s TTL）+ cover 缓存（ConcurrentHashMap，负缓存 60s TTL）
- 章节列表处理使用 `async/awaitAll` 并行化
- 批量删除使用并行化

### 安全

- 不要引入 OWASP Top 10 漏洞
- 文件操作使用 `UniFile`（SAF 安全框架）
- 数据库操作使用 SQLDelight 类型安全查询
