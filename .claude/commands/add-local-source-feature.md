# 添加本地图源功能

## 触发条件

当需要修改本地图源（Local Source）相关功能时执行。本地图源的数据流和远程图源不同，需要特别注意。

## 本地图源架构

```
文件系统 (UniFile)
  ↓ 扫描
LocalMangaSyncService.sync()  ← 将文件系统状态同步到 DB
  ↓ 写入
SQLite (mangas 表, source = 0)
  ↓ 订阅
GetLocalManga.subscribe(query)  ← Flow<List<Manga>>
  ↓ 收集
BrowseSourceScreenModel.localMangaFlow
  ↓ 渲染
LocalBrowseSourceContent (Compose)
```

**关键区别**：本地图源**不使用 Paging**，直接从 DB Flow 获取 `List<Manga>` 渲染。

## 涉及的关键文件

| 层 | 文件 | 说明 |
|----|------|------|
| 文件系统 | `source-local/.../io/LocalSourceFileSystem.kt` | expect/actual，文件操作 |
| 图源 | `source-local/.../LocalSource.kt` | actual 实现，缓存、搜索、章节解析 |
| 封面 | `source-local/.../image/LocalCoverManager.kt` | 封面缓存（ConcurrentHashMap） |
| 同步 | `source-local/.../LocalMangaSyncService.kt` | 文件→DB 同步 |
| ScreenModel | `app/.../browse/source/browse/BrowseSourceScreenModel.kt` | 状态管理 |
| Screen | `app/.../browse/source/browse/BrowseSourceScreen.kt` | UI + 事件 |
| 内容 | `app/.../presentation/browse/BrowseSourceScreen.kt` | `LocalBrowseSourceContent` |
| 列表/网格 | `app/.../presentation/browse/components/BrowseSource*.kt` | 有 List<Manga> 重载 |

## 步骤

### Step 1: 理解数据流

1. 阅读 `LocalMangaSyncService.sync()` — 了解文件系统→DB 同步逻辑
2. 阅读 `BrowseSourceScreenModel` — 注意 `isLocalSource` 分支
3. 阅读 `LocalBrowseSourceContent` — 了解本地图源专用 UI

### Step 2: 确定改动位置

**文件操作相关**：修改 `LocalSourceFileSystem`（expect + actual）
**缓存相关**：修改 `LocalSource`（目录缓存）或 `LocalCoverManager`（封面缓存）
**DB 操作相关**：修改 `.sq` + Repository + Interactor
**UI 相关**：修改 `BrowseSourceScreenModel` + `BrowseSourceScreen`

### Step 3: KMP expect/actual

如果需要新增 `LocalSourceFileSystem` 或 `LocalCoverManager` 方法：

1. 在 `commonMain` 的 expect class 中添加方法声明
2. 在 `androidMain` 的 actual class 中添加实现
3. **两端必须同步**，否则编译失败

### Step 4: 缓存一致性

修改文件系统后，必须确保：

```kotlin
// 1. 清除目录缓存和封面缓存
(source as? LocalSource)?.invalidateCache()
// 2. 重新同步文件系统状态到 DB
doSyncLocalManga()  // 或 syncLocalManga()
```

**sync 使用 Mutex 保护**，不会并发执行。

### Step 5: 验证

```bash
./gradlew :app:compileDebugKotlin
```

## 检查清单

- [ ] expect/actual 方法签名一致
- [ ] 文件操作后调用了 `invalidateCache()` + `sync()`
- [ ] 缓存更新是线程安全的（`@Volatile`、`ConcurrentHashMap`、`Mutex`）
- [ ] DB 操作通过 Repository 接口（不直接写 SQL）
- [ ] 编译通过

## 常见陷阱

- `UniFile.delete()` 不能删除非空目录，需要递归删除
- 负缓存（cover 不存在）有 60s TTL，会自动过期
- `sync()` 末尾会调用 `invalidateCache()`，删除流程中不需要重复调用（但也不会有害）
- `deleteLocalMangaById` SQL 有 `AND favorite = 0` 条件，需要先 unfavorite
