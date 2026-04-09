# 在现有模块中添加功能

## 触发条件

当用户要求在已有功能模块（如 Library、Browse、Manga、Reader 等）中添加新功能时执行。

## 步骤

### Step 1: 理解现有模块

**必须先读，后写。** 按以下顺序阅读：

1. 浏览模块目录结构（`app/.../ui/{module}/` + `app/.../presentation/{module}/`）
2. 阅读 ScreenModel（State 结构、Dialog 枚举、现有方法）
3. 阅读 Screen（Compose UI 结构、事件绑定）
4. 阅读相关 Interactor / Repository 接口
5. 如涉及数据库，阅读 `.sq` 文件中的相关查询

**关键产出**：动手前明确该模块的状态管理模式、数据流向、现有的 Dialog 类型。

### Step 2: 判断改动范围

| 功能类型 | 需要改动/新增的文件 |
|----------|---------------------|
| 新增交互（按钮/操作） | ScreenModel 加方法 + State/Dialog，Screen 加 UI |
| 新增数据库操作 | `.sq` 加查询 + Repository 接口/实现 + Interactor + DI 注册 |
| 新增 UI 组件 | `presentation/{module}/components/` 下新建 Composable |
| 新增对话框 | Dialog sealed interface 加 case，Screen 加 when 分支 |
| 新增国际化文案 | `i18n/.../base/strings.xml` 加 string 条目 |

### Step 3: 数据层改动（如需要）

1. 在 `.sq` 文件中添加查询
2. 在 `domain/.../repository/` 接口中添加方法
3. 在 `data/.../` 实现类中添加实现
4. 如需新 Interactor，在 `domain/.../interactor/` 下创建
5. 在 `DomainModule.kt` 中注册 DI

### Step 4: ScreenModel 改动

- State 新增字段用 `@Immutable data class` 的 `copy()` 更新
- 新增 Dialog 类型在 `sealed interface Dialog` 中
- 副作用操作用 `screenModelScope.launchIO {}` 或 `launchNonCancellable {}`
- 需要的依赖通过构造函数 `= Injekt.get()` 注入

### Step 5: Screen / Compose UI 改动

- Dialog 处理在 `when (state.dialog)` 中添加分支
- 新 Composable 放在 `presentation/{module}/components/`
- 回调通过参数传递，不在子组件内处理业务逻辑

### Step 6: 验证

```bash
./gradlew :app:compileDebugKotlin
```

## 检查清单

- [ ] 阅读了模块现有代码，理解了已有模式
- [ ] 新代码与模块已有代码风格一致
- [ ] State 更新使用 `mutableState.update { it.copy(...) }`
- [ ] 异步操作使用正确的 coroutine scope
- [ ] 国际化文案添加到 `base/strings.xml`
- [ ] 编译通过

## 关键原则

- **模块内一致性优先**：现有模块写法不完全符合新规范时，保持一致
- **改动最小化**：能扩展现有文件的，不新建
- **先搜索再创建**：不确定时先搜索模块内其他类似实现
