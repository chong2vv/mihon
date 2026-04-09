# 创建新页面（Screen + ScreenModel）

## 触发条件

当需要创建一个全新的页面时执行。

## 步骤

### Step 1: 确定页面位置

```
新页面属于哪个业务模块？
├─ 已有模块（browse/library/manga/reader/more）→ 放在对应目录
└─ 全新模块 → 在 ui/ 和 presentation/ 下各新建目录
```

### Step 2: 创建 ScreenModel

在 `app/.../ui/{module}/` 下创建：

```kotlin
class XxxScreenModel(
    // 依赖通过构造函数注入
    private val xxxInteractor: XxxInteractor = Injekt.get(),
) : StateScreenModel<XxxScreenModel.State>(State()) {

    init {
        // 初始化加载
    }

    // --- 公开方法 ---

    fun setDialog(dialog: Dialog?) {
        mutableState.update { it.copy(dialog = dialog) }
    }

    // --- 私有方法 ---

    // --- 类型定义 ---

    sealed interface Dialog {
        // data class XxxDialog(...) : Dialog
    }

    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val dialog: Dialog? = null,
    )
}
```

### Step 3: 创建 Screen

在 `app/.../ui/{module}/` 下创建：

```kotlin
class XxxScreen : Screen() {

    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { XxxScreenModel() }
        val state by screenModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.xxx),
                    navigateUp = { navigator.pop() },
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { paddingValues ->
            // 内容
        }

        // Dialog 处理
        when (val dialog = state.dialog) {
            // ...
            else -> {}
        }
    }
}
```

### Step 4: 创建 Compose Content

如果 UI 较复杂，在 `app/.../presentation/{module}/` 下拆分 Composable：

```kotlin
@Composable
fun XxxContent(
    state: XxxScreenModel.State,
    contentPadding: PaddingValues,
    onItemClick: (Item) -> Unit,
    // ... 回调参数
) {
    // UI 实现
}
```

### Step 5: 注册导航

在需要跳转的地方：

```kotlin
navigator.push(XxxScreen())
```

## 检查清单

- [ ] ScreenModel 继承 `StateScreenModel`
- [ ] State 标注 `@Immutable`
- [ ] Screen 继承 `Screen()`
- [ ] 使用 presentation-core 的 `Scaffold`
- [ ] Dialog 用 sealed interface
- [ ] 编译通过
