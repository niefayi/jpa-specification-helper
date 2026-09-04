# jpa-specification-helper

**English** · [中文](README.zh-CN.md)

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

### 3. Specifications are type-unsafe

`root.get("name")` is just a magic string — the compiler cannot tell whether
`"name"` exists on `User`, whether it matches the entity attribute's type, or
whether the value you compare it with is compatible. A typo like `"nem"` only
fails at query time (or silently matches nothing); a `LIKE` on a `Date` field
blows up at runtime too.

With `@EntityCondition(entity = User.class)` the same paths are validated against
the entity at **compile time**, and the generated `UserFields` constants make them
refactor-safe and IDE auto-completable:

```java
@EntityCondition(entity = User.class)
public class UserCondition {
    @Select(value = UserFields.name, type = SelectTypeEnum.LIKE)
    private String name;

    @Select(value = UserFields.dept.name, type = SelectTypeEnum.LIKE)
    private String deptName;
}
```

## Features

- One `@Select` annotation describes the query semantics; `SelectTypeEnum` covers all common operations
- Non-null fields participate; empty collections are ignored automatically
- `@ConditionGroup` nested classes express multi-condition `AND` / `OR` grouping, arbitrarily nested
- `@Select(value = "a.b.c")` dotted paths auto-join associations, with join caching
- **Ordering** — `@OrderBy` on a condition field (dotted path + direction + priority), or a Spring
  `Sort` via `Specifications.orderBy(Sort)`
- **Spring-style composition** — `Specifications.where(c).and(c).or(spec).not().build()` chains condition
  POJOs and hand-written `Specification`s, all null-safe
- **Type-safe**: bind a condition to its entity with `@EntityCondition(entity = ...)` — a bundled compile-time processor
  validates every `@Select` and `@OrderBy` path and value type against the entity (unknown fields / wrong types fail the build),
  and generates `<Entity>Fields` constants for IDE auto-completion (`@Select(value = UserFields.dept.name)`)
- **Zero runtime overhead**: the processor is compile-time only; at runtime the behaviour is byte-for-byte identical to
  the plain string-path version
- Dual variants: JPA 2 (`javax.persistence`) and JPA 3 (`jakarta.persistence`), sharing one source
- Zero runtime dependencies (JPA only, `provided`)
- Field caching, performance friendly

## Quick Start

### Dependency

Pick the variant that matches your JPA / Spring Boot version:

- **JPA 2 / Spring Boot 2** (`javax.persistence`):

```xml
<dependency>
    <groupId>io.github.niefayi</groupId>
    <artifactId>jpa-specification-helper</artifactId>
    <version>1.3.1</version>
</dependency>
```

- **JPA 3 / Spring Boot 3** (`jakarta.persistence`):

```xml
<dependency>
    <groupId>io.github.niefayi</groupId>
    <artifactId>jpa-specification-helper-jakarta</artifactId>
    <version>1.3.1</version>
</dependency>
```

Both variants share the same API and sources; only the JPA API package differs.

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

### Compose & order (Spring-style)

`Specifications` mirrors Spring Data's `Specification.where(...).and(...).or(...)`
but accepts condition POJOs alongside hand-written specifications. Every call is
immutable and null-safe (a `null` operand contributes nothing; an empty condition
matches everything):

```java
Specification<User> spec = Specifications.<User>where(userCondition)
        .and(anotherCondition)
        .or((root, q, cb) -> cb.equal(root.get("status"), 1))
        .not()
        .orderBy(Sort.by(Sort.Direction.DESC, "age"))
        .build();
```

The result is a plain `Specification`, usable directly with
`JpaSpecificationExecutor`.

Ordering can also be declared on the condition itself with `@OrderBy`. Enable it
on the helper via the fluent builder:

```java
public class UserCondition {
    @Select(type = SelectTypeEnum.LIKE)
    private String name;

    @OrderBy(value = UserFields.dept.name, direction = OrderDirection.ASC, priority = 0)
    private Boolean sortByDept;   // any non-null value turns the ordering on

    @OrderBy(value = "age", direction = OrderDirection.DESC, priority = 1)
    private Boolean sortByAge;
}

SpecificationHelper helper = SpecificationHelper.builder()
        .distinct(true)   // optional: prepend SetDistinctStage
        .orderBy(true)    // optional: apply @OrderBy fields
        .build();
```

`@OrderBy` fields participate only when non-null, apply dotted join paths, and
multiple fields sort by `priority` (lower first, then declaration order). Both
`@Select` and `@OrderBy` paths are validated at compile time when the condition
is bound with `@EntityCondition`.

## Type-safe conditions (entity binding)

Bind a condition to its JPA entity with `@EntityCondition(entity = ...)` on the root
condition class. A compile-time annotation processor (bundled in the same jar and
auto-discovered by `javac`) then:

> **What it runs on.** The processor is an AST-based javac annotation processor: it reads
> your condition / entity classes from the compiler's AST through the standard
> `javax.lang.model` API (`Elements` / `Types`), and writes the generated `*Fields`
> interfaces back into the compilation via `Filer`. It validates and generates new source
> files — it never rewrites your own classes.

- **validates every `@Select`** against the entity — the path must exist (traversed
  through joinable associations only; `@Embedded` segments are rejected because the
  runtime joins every path segment), and the condition field's type must be
  compatible with the entity attribute for the given operator. Any mismatch fails
  the build with a precise message:

  ```
  @Select path "dept.nam" — field "nam" not found on entity User, did you mean "name"?
  @Select LIKE on path "age" requires a String attribute, but the entity attribute is java.lang.Integer
  ```
- **generates `<Entity>Fields` constants** (one interface per `@Entity`, in the entity's
  package) whose members mirror the persistent attributes and association paths, so the
  IDE auto-completes and refactor-safe paths are typed:

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

The generated constants look like:

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

Deep association paths auto-complete one segment at a time — bind the condition to the
entity that **owns** the path (here `users` belongs to `Dept`, not `User`):

```java
@EntityCondition(entity = Dept.class)
public class DeptUserNicknameCondition {
    // type DeptFields. -> users -> profile -> nickname
    @Select(value = DeptFields.users.profile.nickname, type = SelectTypeEnum.EQ)
    private String nickname;
}
```

Notes:

- Nested `@ConditionGroup` classes are validated against the same entity root, so the
  annotation is only needed on the root class.
- Conditions without `@EntityCondition` are left untouched — fully backward compatible.
- **IDE auto-completion**: enable *Annotation Processing* for the project (IntelliJ IDEA
  enables it by default, and it stays on when building via Maven — the library's own
  `<proc>none</proc>` does not leak into consumer builds). After the first build the
  generated `*Fields` interfaces appear under `target/generated-sources/annotations`; if
  the IDE does not pick them up, right-click that directory → *Mark Directory as →
  Generated Sources Root* and rebuild. Then typing `DeptFields.` auto-completes
  `users`, then `profile`, then `nickname`.
- Constants are generated from the `@Entity` classes compiled in the module. If your
  entities live in a separate (dependency) module, either add this artifact there too so
  its `Fields` constants are generated, or keep using string paths — validation still runs
  against entities from dependency jars.

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
  `LEFT`/`INNER`/`RIGHT` (default `LEFT`, can also be set per segment). When an
  explicit `joinType[]` is given it must match the number of join segments in the
  path; otherwise it falls back to the first type.
- `resolver()`: custom operator strategy, takes precedence over `type()`, see below.

> **Note on `distinct`**: the built-in pipeline unconditionally enables
> `distinct(true)` to keep join results free of duplicates. This is a deliberate
> trade-off; if your query needs to keep duplicate rows, assemble a custom
> `SpecificationPipeline` without the `SetDistinctStage`.

## Extensibility

The library is built around a few extension points, so you can grow it without forking.

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
        // from.get(fieldName), cb.*, ...
    }
}
```

Strategies are stateless singletons, cached by class and thread-safe. If a resolver
needs dependencies or shared state, register concrete instances on a
`SelectPredicateResolverRegistry` and inject it into the helper:

```java
SelectPredicateResolverRegistry registry = new SelectPredicateResolverRegistry();
registry.register(MyResolver.class, myResolverWithDependencies);

SpecificationHelper helper = SpecificationHelper.builder().registry(registry).build();
```

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

**Condition processing is protected.** Turning the condition object into predicates
can never be silently skipped:

- The constructor **auto-appends** a `ConditionProcessor` when the given list contains
  no `ConditionProcessorStage` — the marker interface that every processor implements,
  so a custom processor is protected too.
- `SpecificationPipeline.requireConditionProcessor(stages)` is the strict variant: it
  throws `IllegalArgumentException` when no such stage is present.

`SetDistinctStage` is deliberately optional — leave it out to keep duplicate rows (see
the note on `distinct` above).

### 3. Customise the traversal (template method)

`ConditionProcessor` inherits its algorithm from `AbstractConditionProcessor`. To
customise *how* conditions are turned into predicates — without losing the pipeline
protection — extend the base class and override its protected hooks:

```java
public class NameOnlyProcessor extends AbstractConditionProcessor {
    @Override
    protected boolean shouldProcessField(Field field, Object value) {
        return field.getName().equals("name");
    }
}
```

Available hooks: `shouldProcessField`, `isMeaningful`, `combinePredicates`,
`resolveJoinTarget`, `resolveResolver`, `resolveFieldName`. The overall traversal
(`process`) is final, so a subclass can tune every step but not break the pipeline
contract.

### 4. Fluent helper builder

`SpecificationHelper.builder()` assembles a helper from declarative flags and custom
stages — a convenient alternative to building a `SpecificationPipeline` by hand:

```java
SpecificationHelper helper = SpecificationHelper.builder()
        .distinct(true)                    // prepend SetDistinctStage
        .orderBy(true)                     // append OrderByStage (@OrderBy support)
        .stage(new MyStage())              // custom stages run after condition processing
        .registry(customRegistry)          // resolver registry
        .build();
```

For most use cases the built-in pipeline is enough — just use the shared singleton
`SpecificationHelper.DEFAULT`.

### Extension points at a glance

| Extension point | Interface / entry | What it extends |
| --------------- | ----------------- | --------------- |
| Predicate operator | `SelectPredicateResolver` + `@Select.resolver()` | new query operators |
| Resolver instances | `SelectPredicateResolverRegistry` | resolver dependency / shared state |
| Pipeline stage | `SpecificationStage` + `SpecificationPipeline` | new build steps (distinct, transformation, ...) |
| Condition traversal | `AbstractConditionProcessor` (template method) | how the condition object becomes predicates |
| Helper assembly | `SpecificationHelper.builder()` | declarative pipeline configuration |
| Query composition | `Specifications` | chaining conditions / specifications, sorting |
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

Builds both variants (`jpa-specification-helper` for JPA 2 and
`jpa-specification-helper-jakarta` for JPA 3). Requires JDK 17+ because the
jakarta variant's tests run on Hibernate 6.

```bash
mvn clean install
```

## License

[Apache License 2.0](LICENSE)
