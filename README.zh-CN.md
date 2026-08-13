# morphling-jpa-specification-builder

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
- **可扩展** —— 通过 `@Select.resolver()` 注入自定义操作符，不用改库本身

## 特性

- 一个 `@Select` 注解描述查询语义，`SelectTypeEnum` 覆盖全部常用操作
- 字段非空才参与条件拼接，空集合自动忽略
- `@ConditionGroup` 嵌套类表达多条件 `AND` / `OR` 分组，任意嵌套
- `@Select(value = "a.b.c")` 点号路径自动关联查询，join 结果缓存
- 零运行时第三方依赖（仅 JPA，`provided`）
- 字段缓存，性能友好

## 快速开始

### 引入依赖

```xml
<dependency>
    <groupId>io.github.morphling</groupId>
    <artifactId>morphling-jpa-specification-builder</artifactId>
    <version>1.0.0</version>
</dependency>
```

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

## 自定义操作符（扩展）

`SelectTypeEnum` 只内置通用 SQL 操作符。业务相关的操作符（如按天 / 月过滤的日期区间）
通过实现 `SelectPredicateResolver` 注入，用 `@Select.resolver()` 引用：

```java
@Select(value = "birthday", resolver = AroundDayResolver.class)
private Date birthdayDay;
```

内置的 `AroundDayResolver` / `AroundMonthResolver`（`io.github.morphling.jpa.extension` 包）
即按此机制实现。自定义策略只需实现接口的无参构造类：

```java
public class MyResolver implements SelectPredicateResolver {
    @Override
    public Predicate getPredicate(From<?, ?> from, CriteriaBuilder cb, String fieldName, Object fieldObject) {
        // ...
    }
}
```

策略为无状态单例，按类缓存、线程安全。

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

```bash
mvn clean install
```

## License

[Apache License 2.0](LICENSE)
