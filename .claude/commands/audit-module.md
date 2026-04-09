# 模块审计

## 触发条件

当用户要求审计、检查、review 某个模块的代码质量时执行。

**本命令只做诊断，不修改代码。**

## 步骤

### Step 1: 读取模块全部代码

1. 确定模块范围（如 `browse`、`library`、`manga`）
2. 搜索 `app/.../ui/{module}/` 和 `app/.../presentation/{module}/` 下的所有文件
3. 逐个阅读，统计文件行数

### Step 2: 按维度逐项检查

#### 2.1 架构分层

| 检查项 | 合规标准 |
|--------|----------|
| ScreenModel 不引用 Compose/UI | 无 `import androidx.compose` |
| Screen 不包含业务逻辑 | 逻辑在 ScreenModel 中 |
| Interactor 封装了用例 | 不直接在 ScreenModel 中调用 Repository |
| Repository 接口在 domain/ | 实现在 data/ |

#### 2.2 State 管理

| 检查项 | 合规标准 |
|--------|----------|
| State 标注 `@Immutable` | `data class State` 有注解 |
| State 通过 `copy()` 更新 | 不直接修改字段 |
| Dialog 是 sealed interface | 类型安全的弹窗管理 |
| 选中状态用 `Set<Long>` | 不用 `List` 或 `MutableSet` |

#### 2.3 Compose UI

| 检查项 | 合规标准 |
|--------|----------|
| 使用 presentation-core 的 Scaffold | 不是 material3 的 |
| 回调向下传递 | 子组件不处理导航 |
| Modifier 是第一个可选参数 | |
| LaunchedEffect 处理副作用 | 不在 Composition 中启动协程 |

#### 2.4 并发安全

| 检查项 | 合规标准 |
|--------|----------|
| 共享缓存线程安全 | `ConcurrentHashMap` / `@Volatile` |
| 不可取消操作 | 删除用 `launchNonCancellable` |
| 防止并发 | 重复操作用 `Mutex` 保护 |

#### 2.5 数据库

| 检查项 | 合规标准 |
|--------|----------|
| SQL 在 `.sq` 文件中定义 | 不硬编码 SQL |
| DELETE 有 WHERE 条件 | 防止误删 |
| Flow 用于响应式订阅 | `suspend` 用于一次性操作 |

#### 2.6 国际化

| 检查项 | 合规标准 |
|--------|----------|
| 用户可见文案在 strings.xml | 不硬编码字符串 |
| 使用 `stringResource(MR.strings.xxx)` | |

### Step 3: 输出审计报告

```
## {Module} 模块审计报告

### 模块概览
| 文件 | 行数 | 角色 |
|------|------|------|

### 合规项

### 违规项

#### 必须修复（架构违规、崩溃风险、数据安全）
（问题描述 + 文件:行号 + 违反的规则 + 修复方向）

#### 建议修复（规范偏差）

#### 可选优化

### 后续建议
```

## 注意事项

- **只读审计**：绝不修改任何代码
- **引用规则**：每个违规项标注违反了 CLAUDE.md 哪条规则
- **关注实际影响**：区分"不优雅但能用"和"会出 bug"
