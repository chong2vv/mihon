# 添加数据库查询

## 触发条件

当需要新增数据库操作（查询、插入、更新、删除）时执行。

## 步骤

### Step 1: 确定操作的表和查询类型

浏览 `data/src/main/sqldelight/tachiyomi/data/` 目录，找到对应的 `.sq` 文件。

主要表文件：
- `mangas.sq` — 漫画表
- `chapters.sq` — 章节表
- `categories.sq` — 分类表
- `mangas_categories.sq` — 漫画-分类关联表
- `history.sq` — 阅读历史表
- `extension_repos.sq` — 插件仓库表

### Step 2: 在 `.sq` 文件中添加查询

```sql
-- 命名格式：动词 + 实体 + 条件（驼峰命名）
getLocalMangaBySourceId:
SELECT *
FROM mangas
WHERE source = 0;
```

注意：
- 查询名必须唯一
- 参数用 `:paramName` 格式
- DELETE/UPDATE 加适当的 WHERE 条件防止误操作

### Step 3: 添加 Repository 接口方法

在 `domain/src/main/java/tachiyomi/domain/.../repository/` 对应接口中添加：

```kotlin
// 响应式订阅用 Flow
fun getXxxAsFlow(): Flow<List<Xxx>>

// 一次性操作用 suspend
suspend fun getXxx(): List<Xxx>
suspend fun deleteXxx(id: Long)
```

### Step 4: 添加 Repository 实现

在 `data/src/main/java/tachiyomi/data/.../` 对应实现类中添加：

```kotlin
override suspend fun deleteXxx(id: Long) {
    handler.await { xxxQueries.deleteXxxById(id) }
}

override fun getXxxAsFlow(): Flow<List<Xxx>> {
    return handler.subscribeToList { xxxQueries.getXxx(XxxMapper::mapXxx) }
}
```

### Step 5: 添加 Interactor（如需要）

如果是新的独立用例，在 `domain/.../interactor/` 下创建：

```kotlin
class GetXxx(
    private val repository: XxxRepository,
) {
    fun subscribe(): Flow<List<Xxx>> = repository.getXxxAsFlow()
    suspend fun await(): List<Xxx> = repository.getXxx()
}
```

### Step 6: 注册 DI

在 `app/.../domain/DomainModule.kt` 中添加：

```kotlin
addFactory { GetXxx(get()) }
```

如果是新 Repository 实现，还需注册：

```kotlin
addSingletonFactory<XxxRepository> { XxxRepositoryImpl(get()) }
```

### Step 7: 验证

```bash
./gradlew :app:compileDebugKotlin
```

## 检查清单

- [ ] `.sq` 查询语法正确
- [ ] Repository 接口和实现方法签名一致
- [ ] DI 注册完整
- [ ] 编译通过
