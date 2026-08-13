# jpa-specification-helper

Annotation-driven dynamic JPA Specification builder.

Write a query once, reuse it everywhere. Define conditions declaratively on a plain
POJO, and this library turns it into a type-safe
`org.springframework.data.jpa.domain.Specification` for you.

## Why this library?

### 1. Writing a `Specification` by hand is painful

A typical Spring Data dynamic query needs a lot of ceremony:

```java
Specification<User> spec = (root, query, cb) -> {
    List<Predicate> predicates = new ArrayList<>();
    if (name != null) {
        predicates.add(cb.like(root.get("name"), "%" + name + "%"));
    }
    if (age != null) {
        predicates.add(cb.greaterThanOrEqualTo(root.get("age"), age));
    }
    // ... every field repeats the same null-check + predicate pattern
    return cb.and(predicates.toArray(new Predicate[0]));
};
```

- Field names are magic strings the compiler cannot check.
- Null-checking and predicate composition are manual, repetitive and easy to get wrong.
- Adding a condition means touching the query method again.

With this library, the same query is just an annotated POJO:

```java
public class UserCondition {
    @Select(type = SelectTypeEnum.LIKE) private String name;
    @Select(type = SelectTypeEnum.GTE) private Integer age;
}

Specification<User> spec = SpecificationHelper.DEFAULT.buildSpecification(condition);
```

### 2. Reuse the queries you have already built

The condition object *is* the reusable artifact:

- **One definition, many entry points** — the same condition drives list, count,
  export, etc., with zero duplicated logic.
- **Frontend → backend** — conditions are plain POJOs, so the JSON the frontend
  sends deserializes straight into them.
- **Compose & nest** — `@ConditionGroup` builds AND / OR trees out of conditions
  without writing a single predicate.
- **Extend** — plug in custom predicate operators via `@Select.resolver()` or add
  build steps through a custom `SpecificationPipeline`; no forking required.

## Features

- One `@Select` annotation describes the query semantics; `SelectTypeEnum` covers all common operations
- Non-null fields participate; empty collections are ignored automatically
- `@ConditionGroup` nested classes express multi-condition `AND` / `OR` grouping, arbitrarily nested
- `@Select(value = "a.b.c")` dotted paths auto-join associations, with join caching
- Zero runtime dependencies (JPA only, `provided`)
- Field caching, performance friendly

## Quick Start

### Dependency

```xml
<dependency>
    <groupId>io.github.morphling</groupId>
    <artifactId>jpa-specification-helper</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Define a condition

```java
public class UserCondition {
    @Select(type = SelectTypeEnum.LIKE)
    private String name;

    @Select(type = SelectTypeEnum.GTE)
    private Integer age;
}
```

### Build a query

```java
UserCondition condition = new UserCondition();
condition.setName("Zhang");
condition.setAge(18);

Specification<User> specification = SpecificationHelper.DEFAULT.buildSpecification(condition);
List<User> list = userRepository.findAll(specification);
```

A condition is only appended when the `@Select` field value is non-null (or a
non-empty collection); fields without `@Select` are always ignored.

### From frontend JSON

Because conditions are plain POJOs, the JSON a frontend sends can be deserialized
directly and then built into a query:

```java
// {"name":"Zhang","ageGte":18,"ids":[1,2]}
UserCondition condition = objectMapper.readValue(json, UserCondition.class);
Specification<User> spec = SpecificationHelper.DEFAULT.buildSpecification(condition);
```

## Query types supported by @Select

Specified via `type()`, see `SelectTypeEnum`:

| Type | Description |
| ---- | ----------- |
| `EQ` / `NE` | equals / not equals |
| `LIKE` / `L_LIKE` / `R_LIKE` | fuzzy match (both / left / right) |
| `GT` / `GTE` / `LT` / `LTE` | greater than / greater or equal / less than / less or equal |
| `IN` / `NOT_IN` | in / not in a collection |
| `BETWEEN` | range; the value must be a `List` of length 2 |
| `IS_NULL` / `NOT_NULL` | is null / is not null |
| `IGNORE` | ignored; no condition is generated |

- `value()`: when empty it targets the current field directly; when a dotted path
  (e.g. `"dept.name"`) it auto-joins the association, with `joinType()` selecting
  `LEFT`/`INNER`/`RIGHT` (default `LEFT`, can also be set per segment).
- `resolver()`: custom operator strategy, takes precedence over `type()`, see below.

## Extensibility

The library is built around two extension points, so you can grow it without forking.

### 1. Custom predicate operators

`SelectTypeEnum` only ships common SQL operators. Business-specific operators (such
as date-range filtering by day / month) are injected by implementing
`SelectPredicateResolver` and referenced via `@Select.resolver()`:

```java
@Select(value = "birthday", resolver = AroundDayResolver.class)
private Date birthdayDay;
```

A custom strategy is just a class with a no-arg constructor:

```java
public class MyResolver implements SelectPredicateResolver {
    @Override
    public Predicate getPredicate(From<?, ?> from, CriteriaBuilder cb, String fieldName, Object fieldObject) {
        // ...
    }
}
```

Strategies are stateless singletons, cached by class and thread-safe.

### 2. Custom pipeline stages

The whole build process is a pipeline of stages sharing a `SpecificationContext`.
Implement `SpecificationStage` and assemble your own `SpecificationPipeline`, then
inject it into `SpecificationHelper`:

```java
SpecificationPipeline pipeline = new SpecificationPipeline(Arrays.asList(
        new SetDistinctStage(),
        new MyStage(),      // your custom stage
        new ConditionProcessor()
));
SpecificationHelper helper = new SpecificationHelper(pipeline);
```

```java
public class MyStage implements SpecificationStage {
    @Override
    public void process(SpecificationContext context) {
        // read / mutate the shared context, e.g. context.getQuery(), context.getResult()
    }
}
```

For most use cases the built-in pipeline is enough — just use the shared singleton
`SpecificationHelper.DEFAULT`.

### Extension points at a glance

| Extension point | Interface / entry | What it extends |
| --------------- | ----------------- | --------------- |
| Predicate operator | `SelectPredicateResolver` + `@Select.resolver()` | new query operators |
| Pipeline stage | `SpecificationStage` + `SpecificationPipeline` | new build steps (distinct, transformation, ...) |
| Entry point | `SpecificationHelper` (or `SpecificationHelper.DEFAULT`) | how a query is built |

## Grouping example

A field belongs to a group simply by being declared inside a `@ConditionGroup`
nested class; nesting between groups is expressed through object composition, with
no string naming required:

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

Semantics: `name LIKE ? AND (age >= ? OR age <= ?)`. Nesting is unlimited:

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

i.e. `name = ? OR (age >= ? AND status != ?)`.

## Build

```bash
mvn clean install
```

## License

[Apache License 2.0](LICENSE)
