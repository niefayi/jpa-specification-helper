# jpa-specification-helper

[English](README.md) · **中文**

注解驱动的动态 JPA Specification 构建器。

查询只写一次，到处复用。把查询条件声明式地定义在一个普通 POJO 上，由本库自动生成类型安全的
`org.springframework.data.jpa.domain.Specification`。

## 为什么需要这个库？

### 1. 手写 Specification 非常痛苦

一个典型的 Spring Data 动态查询需要大量样板代码：

```java
Specification<User> spec = (root, query, cb) -> {
    List<Predicate> predicates = new ArrayList<>();
    if (name != null) {
        predicates.add(cb.like(root.get("name"), "%" + name + "%"));
    }
    if (age != null) {
        predicates.add(cb.greaterThanOrEqualTo(root.get("age"), age));
    }
    // ... 每个字段都重复同样的判空 + 拼谓词
    return cb.and(predicates.toArray(new Predicate[0]));
};
```

- 字段名是魔法字符串，编译器检查不了
- 判空和谓词拼接全靠手写，重复且容易出错
- 每加一个条件就要改查询方法

同样的查询，用本库只是一个带注解的 POJO：

```java
public class UserCondition {
    @Select(type = SelectTypeEnum.LIKE) private String name;
    @Select(type = SelectTypeEnum.GTE) private Integer age;
}

Specification<User> spec = SpecificationHelper.DEFAULT.buildSpecification(condition);
```

### 2. 重用已经构造好的查询

条件对象本身就是可复用的资产：

- **一处定义，多处使用** —— 同一个条件同时驱动列表 / 计数 / 导出，零重复逻辑
- **前端 → 后端** —— 条件是普通 POJO，前端传回来的 JSON 直接反序列化进条件对象
- **组合与嵌套** —— `@ConditionGroup` 用条件对象拼出 AND / OR 布尔树，不用写一行谓词
- **可扩展** —— 通过 `@Select.resolver()` 注入自定义操作符，或通过自定义 `SpecificationPipeline`
  增加构建步骤，都不用改库本身

### 3. Specification 类型不安全

`root.get("name")` 只是魔法字符串 —— 编译器无法知道 `"name"` 在 `User` 上是否存在、类型是否匹配实体属性、
比较的值是否兼容。拼错成 `"nem"` 只能等到查询时才报错（甚至静默查不到任何结果）；在 `Date` 字段上用 `LIKE`
也是运行时才炸。

用 `@EntityCondition(entity = User.class)` 之后，同样的路径会在**编译期**对照实体校验，生成的 `UserFields`
常量让路径可重构、可被 IDE 自动补全：

```java
@EntityCondition(entity = User.class)
public class UserCondition {
    @Select(value = UserFields.name, type = SelectTypeEnum.LIKE)
    private String name;

    @Select(value = UserFields.dept.name, type = SelectTypeEnum.LIKE)
    private String deptName;
}
```

## 特性

- 一个 `@Select` 注解描述查询语义，`SelectTypeEnum` 覆盖全部常用操作
- 字段非空才参与条件拼接，空集合自动忽略
- `@ConditionGroup` 嵌套类表达多条件 `AND` / `OR` 分组，任意嵌套
- `@Select(value = "a.b.c")` 点号路径自动关联查询，join 结果缓存
- **排序** —— 条件字段上加 `@OrderBy`（点号路径 + 方向 + 优先级），或通过 `Specifications.orderBy(Sort)` 直接传 Spring `Sort`
- **Spring 风格组合** —— `Specifications.where(c).and(c).or(spec).not().build()` 链式混搭条件对象与手写 `Specification`，全程空安全
- **类型安全** —— 用 `@EntityCondition(entity = ...)` 把条件绑定到实体：随 jar 一起发布的编译期处理器会对每个
  `@Select` 和 `@OrderBy` 做校验（路径不存在 / 值类型不匹配直接编译报错），并为实体生成 `<Entity>Fields` 常量用于 IDE 自动补全
  （`@Select(value = UserFields.dept.name)`）
- **零运行时开销** —— 处理器只在编译期生效，运行时行为与纯字符串路径版本完全一致
- 双版本：JPA 2（`javax.persistence`）与 JPA 3（`jakarta.persistence`），共享一份源码
- 零运行时第三方依赖（仅 JPA，`provided`）
- 字段缓存，性能友好

## 快速开始

### 引入依赖

按你的 JPA / Spring Boot 版本选对应变体：

- **JPA 2 / Spring Boot 2**（`javax.persistence`）：

```xml
<dependency>
    <groupId>io.github.anyifei12138</groupId>
    <artifactId>jpa-specification-helper</artifactId>
    <version>1.2.0</version>
</dependency>
```

- **JPA 3 / Spring Boot 3**（`jakarta.persistence`）：

```xml
<dependency>
    <groupId>io.github.anyifei12138</groupId>
    <artifactId>jpa-specification-helper-jakarta</artifactId>
    <version>1.2.0</version>
</dependency>
```

两个变体 API 与源码完全一致，仅 JPA API 包不同。

### 定义查询条件

```java
public class UserCondition {
    @Select(type = SelectTypeEnum.LIKE)
    private String name;

    @Select(type = SelectTypeEnum.GTE)
    private Integer age;
}
```

### 构建查询

```java
UserCondition condition = new UserCondition();
condition.setName("张");
condition.setAge(18);

Specification<User> specification = SpecificationHelper.DEFAULT.buildSpecification(condition);
List<User> list = userRepository.findAll(specification);
```

仅当 `@Select` 字段值非空（集合非空）时，该字段对应的条件才会被拼接；未标注 `@Select` 的字段一律忽略。

### 直接接前端 JSON

因为条件是普通 POJO，前端传来的 JSON 可以直接反序列化再构建查询：

```java
// {"name":"张","ageGte":18,"ids":[1,2]}
UserCondition condition = objectMapper.readValue(json, UserCondition.class);
Specification<User> spec = SpecificationHelper.DEFAULT.buildSpecification(condition);
```

### 组合与排序（Spring 风格）

`Specifications` 复刻 Spring Data 的 `Specification.where(...).and(...).or(...)`，
但既接受条件 POJO，也接受手写 `Specification`。每个调用都不可变、空安全
（`null` 操作数不产生约束；空条件匹配所有）：

```java
Specification<User> spec = Specifications.<User>where(userCondition)
        .and(anotherCondition)
        .or((root, q, cb) -> cb.equal(root.get("status"), 1))
        .not()
        .orderBy(Sort.by(Sort.Direction.DESC, "age"))
        .build();
```

结果仍是普通 `Specification`，可直接交给 `JpaSpecificationExecutor`。

排序也可以直接声明在条件上，用 `@OrderBy`，再通过 fluent builder 开启：

```java
public class UserCondition {
    @Select(type = SelectTypeEnum.LIKE)
    private String name;

    @OrderBy(value = UserFields.dept.name, direction = OrderDirection.ASC, priority = 0)
    private Boolean sortByDept;   // 值非空即启用该排序

    @OrderBy(value = "age", direction = OrderDirection.DESC, priority = 1)
    private Boolean sortByAge;
}

SpecificationHelper helper = SpecificationHelper.builder()
        .distinct(true)   // 可选：前置 SetDistinctStage
        .orderBy(true)    // 可选：应用 @OrderBy 字段
        .build();
```

`@OrderBy` 字段非空才生效、支持点号 join 路径；多个排序字段按 `priority`（小者优先，同级按声明顺序）。
绑定 `@EntityCondition` 后，`@Select` 与 `@OrderBy` 的路径都会被编译期校验。

## 类型安全的条件（实体绑定）

在根条件类上用 `@EntityCondition(entity = ...)` 绑定 JPA 实体。随 jar 打包、由 `javac` 自动发现的编译期注解处理器会：

> **它基于 AST 工作。** 这是一个基于 AST 的 javac 注解处理器：通过标准的 `javax.lang.model` API
> （`Elements` / `Types`）读取你正在编译的条件类 / 实体类的 AST，再通过 `Filer` 把生成的 `*Fields`
> 接口写回编译过程。它只做校验和生成新的源文件，绝不会改写你的类本身。

- **校验每个 `@Select`** —— 路径必须在实体上存在（中间段只能是可 join 的关联；`@Embedded` 段会直接报错，因为运行时对所有中间段做 join），且条件字段类型必须与实体属性在当前
  操作符下兼容；任何不匹配都会让编译失败，并给出精确信息：

  ```
  @Select path "dept.nam" — field "nam" not found on entity User, did you mean "name"?
  @Select LIKE on path "age" requires a String attribute, but the entity attribute is java.lang.Integer
  ```
- **生成 `<Entity>Fields` 常量**（每个 `@Entity` 一个接口，放在实体所在包），成员镜像实体的持久化属性与关联路径，
  让 IDE 自动补全、路径可重构：

```java
@EntityCondition(entity = User.class)
public class UserCondition {
    @Select(value = UserFields.name, type = SelectTypeEnum.LIKE)
    private String name;

    @Select(value = UserFields.dept.name, type = SelectTypeEnum.LIKE)
    private String deptName;

    @Select(value = UserFields.roles.code, type = SelectTypeEnum.IN)
    private List<String> roleCodes;
}
```

生成的常量大致如下：

```java
public interface UserFields {
    String id = "id";
    String name = "name";
    String age = "age";
    interface dept {
        String id = "dept.id";
        String name = "dept.name";
        // ...
    }
    interface roles {
        String code = "roles.code";
        // ...
    }
}
```

深关联路径也是一段一段自动补全的 —— 条件要绑定到**拥有该路径起点**的实体（这里 `users` 属于 `Dept`，不属于 `User`）：

```java
@EntityCondition(entity = Dept.class)
public class DeptUserNicknameCondition {
    // 输入 DeptFields. -> users -> profile -> nickname 逐段自动补全
    @Select(value = DeptFields.users.profile.nickname, type = SelectTypeEnum.EQ)
    private String nickname;
}
```

说明：

- 嵌套的 `@ConditionGroup` 沿用同一实体根进行校验，注解只需标在根类上。
- 没有 `@EntityCondition` 的条件完全不受影响 —— 向后兼容。
- **IDE 自动补全**：请开启工程的 *Annotation Processing*（IntelliJ IDEA 默认开启；走 Maven 构建时也保持开启——本库自身的 `<proc>none</proc>` 不会泄漏到使用方）。首次构建后生成的 `*Fields` 接口出现在 `target/generated-sources/annotations` 下；如果 IDE 没识别，右键该目录 → *Mark Directory as → Generated Sources Root* 再重建。之后输入 `DeptFields.` 就会依次补全 `users`、`profile`、`nickname`。
- 常量由本模块内编译的 `@Entity` 类生成。如果实体在独立的依赖模块里，可把本构件也加进去生成常量，或继续用字符串路径——
  校验对依赖 jar 里的实体同样生效。

## @Select 支持的查询类型

通过 `type()` 指定，见 `SelectTypeEnum`：

| 类型 | 说明 |
| ---- | ---- |
| `EQ` / `NE` | 等于 / 不等于 |
| `LIKE` / `L_LIKE` / `R_LIKE` | 模糊匹配（双侧 / 左侧 / 右侧） |
| `GT` / `GTE` / `LT` / `LTE` | 大于 / 大于等于 / 小于 / 小于等于 |
| `IN` / `NOT_IN` | 集合包含 / 不包含 |
| `BETWEEN` | 区间，值需为长度为 2 的 `List` |
| `IS_NULL` / `NOT_NULL` | 为空 / 非空 |
| `IGNORE` | 忽略，不生成条件 |

- `value()`：为空时直接作用于当前字段；为点号路径（如 `"dept.name"`）时自动关联查询，
  可通过 `joinType()` 指定 `LEFT`/`INNER`/`RIGHT`（默认为 `LEFT`，也可逐段指定）。
- `resolver()`：自定义操作符策略，设置后优先于 `type()`，见下节。

## 扩展性

本库围绕几个扩展点设计，无需 fork 即可按需生长。

### 1. 自定义操作符

`SelectTypeEnum` 只内置通用 SQL 操作符。业务相关的操作符（如按天 / 月过滤的日期区间）
通过实现 `SelectPredicateResolver` 注入，用 `@Select.resolver()` 引用：

```java
@Select(value = "birthday", resolver = AroundDayResolver.class)
private Date birthdayDay;
```

自定义策略只需实现接口的无参构造类：

```java
public class MyResolver implements SelectPredicateResolver {
    @Override
    public Predicate getPredicate(From<?, ?> from, CriteriaBuilder cb, String fieldName, Object fieldObject) {
        // from.get(fieldName)、cb.* 等
    }
}
```

策略为无状态单例，按类缓存、线程安全。若 resolver 需要依赖或共享状态，可在
`SelectPredicateResolverRegistry` 上注册具体实例并注入 helper：

```java
SelectPredicateResolverRegistry registry = new SelectPredicateResolverRegistry();
registry.register(MyResolver.class, myResolverWithDependencies);

SpecificationHelper helper = SpecificationHelper.builder().registry(registry).build();
```

### 2. 自定义流水线阶段

整个构建过程是一条共享 `SpecificationContext` 的流水线。实现 `SpecificationStage`
并组装自己的 `SpecificationPipeline`，再注入 `SpecificationHelper`：

```java
SpecificationPipeline pipeline = new SpecificationPipeline(Arrays.asList(
        new SetDistinctStage(),
        new MyStage(),      // 你的自定义阶段
        new ConditionProcessor()
));
SpecificationHelper helper = new SpecificationHelper(pipeline);
```

```java
public class MyStage implements SpecificationStage {
    @Override
    public void process(SpecificationContext context) {
        // 读取 / 修改共享上下文，如 context.getQuery()、context.getResult()
    }
}
```

**条件处理是受保护的。** 把条件对象转成谓词这一步不能被悄悄跳过：

- 构造函数在传入列表里没有 `ConditionProcessorStage`（所有处理器都实现的标记接口，自定义处理器同样被保护）
  时会**自动在末尾追加**一个 `ConditionProcessor`。
- 想要严格校验可用 `SpecificationPipeline.requireConditionProcessor(stages)`：缺少该阶段会抛 `IllegalArgumentException`。

`SetDistinctStage` 是刻意可选的——去掉它即可保留重复行（见上文 `distinct` 的说明）。

### 3. 定制遍历过程（模板方法）

`ConditionProcessor` 的算法继承自 `AbstractConditionProcessor`。想定制「条件如何转成谓词」
又不想失去流水线保护，就继承基类、覆盖受保护的钩子：

```java
public class NameOnlyProcessor extends AbstractConditionProcessor {
    @Override
    protected boolean shouldProcessField(Field field, Object value) {
        return field.getName().equals("name");
    }
}
```

可用钩子：`shouldProcessField`、`isMeaningful`、`combinePredicates`、`resolveJoinTarget`、
`resolveResolver`、`resolveFieldName`。整体遍历（`process`）是 `final` 的——子类可调每一步，但不会破坏流水线契约。

### 4. Fluent 构建 helper

`SpecificationHelper.builder()` 用声明式开关 + 自定义阶段组装 helper，是手写
`SpecificationPipeline` 的便捷替代：

```java
SpecificationHelper helper = SpecificationHelper.builder()
        .distinct(true)                    // 前置 SetDistinctStage
        .orderBy(true)                     // 追加 OrderByStage（@OrderBy 支持）
        .stage(new MyStage())              // 自定义阶段在条件处理后运行
        .registry(customRegistry)          // resolver 注册表
        .build();
```

大多数场景内置流水线就够用——直接用共享单例 `SpecificationHelper.DEFAULT`。

### 扩展点一览

| 扩展点 | 接口 / 入口 | 扩展什么 |
| ------ | ----------- | -------- |
| 谓词操作符 | `SelectPredicateResolver` + `@Select.resolver()` | 新的查询操作符 |
| resolver 实例 | `SelectPredicateResolverRegistry` | resolver 的依赖注入 / 共享状态 |
| 流水线阶段 | `SpecificationStage` + `SpecificationPipeline` | 新的构建步骤（去重、转换……） |
| 条件遍历 | `AbstractConditionProcessor`（模板方法） | 条件对象如何变成谓词 |
| helper 组装 | `SpecificationHelper.builder()` | 声明式流水线配置 |
| 查询组合 | `Specifications` | 链式组合条件 / 规格、排序 |
| 入口 | `SpecificationHelper`（或 `SpecificationHelper.DEFAULT`） | 查询如何被构建 |

## 分组示例

字段归组的方式是「把字段放进一个 `@ConditionGroup` 嵌套类」，组间嵌套通过对象组合表达，无需任何字符串命名：

```java
public class DemoCondition {
    @Select(type = SelectTypeEnum.LIKE)
    private String name;

    @ConditionGroup(GroupTypeEnum.OR)
    public static class AgeRange {
        @Select(value = "age", type = SelectTypeEnum.GTE)
        private Integer min;

        @Select(value = "age", type = SelectTypeEnum.LTE)
        private Integer max;
    }

    private AgeRange ageRange = new AgeRange();
}
```

语义：`name LIKE ? AND (age >= ? OR age <= ?)`。嵌套不限层数：

```java
@ConditionGroup(GroupTypeEnum.OR)
public static class Outer {
    @Select(value = "name")
    private String name;

    @ConditionGroup(GroupTypeEnum.AND)
    public static class AgeStatus {
        @Select(value = "age", type = SelectTypeEnum.GTE)
        private Integer ageMin;

        @Select(value = "status", type = SelectTypeEnum.NE)
        private Integer statusNot;
    }

    private AgeStatus ageStatus = new AgeStatus();
}
```

即 `name = ? OR (age >= ? AND status != ?)`。

## 构建

一次构建两个变体（JPA 2 的 `jpa-specification-helper` 与 JPA 3 的 `jpa-specification-helper-jakarta`）。
需要 JDK 17+（jakarta 变体的测试跑在 Hibernate 6 上）。

```bash
mvn clean install
```

## License

[Apache License 2.0](LICENSE)
