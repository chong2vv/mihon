# 添加国际化文案

## 触发条件

当需要添加用户可见的文本时执行。

## 步骤

### Step 1: 在 base strings.xml 中添加

文件：`i18n/src/commonMain/moko-resources/base/strings.xml`

```xml
<string name="feature_description">Description text</string>
```

命名规范：
- 使用 snake_case
- 动作类：`action_xxx`（如 `action_retry`、`action_cancel`）
- 标签类：`label_xxx`
- 确认/提示：`xxx_confirm`（如 `delete_manga_confirm`）
- 复数：用 `%d` 占位符（如 `delete_mangas_confirm` 接受 count 参数）

### Step 2: 添加中文翻译（如需要）

文件：`i18n/src/commonMain/moko-resources/zh-rCN/strings.xml`

```xml
<string name="feature_description">描述文本</string>
```

### Step 3: 在代码中使用

Composable 中：
```kotlin
stringResource(MR.strings.feature_description)
// 带参数：
stringResource(MR.strings.delete_mangas_confirm, count)
```

非 Composable 中（需要 Context）：
```kotlin
context.stringResource(MR.strings.feature_description)
```

### Step 4: 验证

```bash
./gradlew :app:compileDebugKotlin
```

## 注意事项

- **禁止**在 UI 中硬编码用户可见文本
- base 语言是英文，其他语言文件只翻译需要的条目
- 已有文案先搜索再使用，避免重复定义
- `MR.strings.xxx` 在编译时生成，添加后需要编译才能引用
