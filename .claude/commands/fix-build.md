# 修复编译错误

## 触发条件

当编译失败需要修复时执行。

## 步骤

### Step 1: 获取完整错误信息

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -50
```

### Step 2: 按错误类型处理

#### Import 冲突 / 重复 import

```
Conflicting import, imported name 'xxx' is ambiguous
```

→ 检查文件中是否有重复 import，删除多余的。注意同名类可能来自不同包。

#### 未解析的引用

```
Unresolved reference: 'xxx'
```

→ 可能原因：
1. 缺少 import — 搜索类名找到正确包路径
2. 拼写错误 — 检查大小写
3. 方法/属性不存在 — 搜索实际命名
4. moko-resources 未生成 — 编译一次 `./gradlew :i18n:generateMRcommonMain`

#### expect/actual 不匹配

```
Actual function 'xxx' has no corresponding expected declaration
```

→ 检查 commonMain 的 expect 和 androidMain 的 actual 方法签名是否完全一致。

#### SQLDelight 错误

```
No column found with name xxx
```

→ 检查 `.sq` 文件中的列名和表结构。

#### Gradle Lock 冲突

```
Could not create service of type BuildLogicUserProvidedBuildServices
Timeout waiting to lock buildLogic
```

→ 执行：
```bash
./gradlew --stop
rm -f .gradle/noVersion/buildLogic.lock
```

### Step 3: 验证修复

```bash
./gradlew :app:compileDebugKotlin
```

## 注意事项

- 不存在 `compileStandardDebugKotlin` 任务，正确的是 `compileDebugKotlin`
- 多模块项目中，错误可能在依赖模块里，注意看完整错误路径
- 修复一个错误可能暴露更多错误，逐步修复
