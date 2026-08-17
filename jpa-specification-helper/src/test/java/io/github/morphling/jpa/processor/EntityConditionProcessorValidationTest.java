package io.github.morphling.jpa.processor;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Compile-time validation tests for {@link EntityConditionProcessor}.
 */
class EntityConditionProcessorValidationTest {

    private static final String USER =
            "import javax.persistence.*;\n" +
            "import java.util.*;\n" +
            "@Entity public class User {\n" +
            "    @Id private Long id;\n" +
            "    private String name;\n" +
            "    private Integer age;\n" +
            "    private Object payload;\n" +
            "    @ManyToOne private Dept dept;\n" +
            "    @ManyToMany private List<Role> roles;\n" +
            "}\n";

    private static final String DEPT =
            "import javax.persistence.*;\n" +
            "import java.util.*;\n" +
            "@Entity public class Dept {\n" +
            "    @Id private Long id;\n" +
            "    private String name;\n" +
            "    @OneToMany(mappedBy = \"dept\") private List<User> users;\n" +
            "}\n";

    private static final String ROLE =
            "import javax.persistence.*;\n" +
            "@Entity public class Role {\n" +
            "    @Id private Long id;\n" +
            "    private String code;\n" +
            "    private String name;\n" +
            "}\n";

    private static final String ADDRESS =
            "import javax.persistence.*;\n" +
            "@Embeddable public class Address {\n" +
            "    private String city;\n" +
            "    private String street;\n" +
            "}\n";

    private static final String REGION =
            "import javax.persistence.*;\n" +
            "@Entity public class Region {\n" +
            "    @Id private Long id;\n" +
            "    private String code;\n" +
            "}\n";

    private static final String CUSTOMER =
            "import javax.persistence.*;\n" +
            "@Entity public class Customer {\n" +
            "    @Id private Long id;\n" +
            "    private String name;\n" +
            "    @Embedded private Address address;\n" +
            "    @ManyToOne private Region region;\n" +
            "}\n";

    private static final String RESOLVER =
            "import io.github.morphling.jpa.SelectPredicateResolver;\n" +
            "import javax.persistence.criteria.*;\n" +
            "public class MyResolver implements SelectPredicateResolver {\n" +
            "    public Predicate getPredicate(From<?, ?> from, CriteriaBuilder cb, String fieldName, Object value) {\n" +
            "        return cb.isNotNull(from.get(fieldName));\n" +
            "    }\n" +
            "}\n";

    private static CompileTestUtil.Result compileCondition(String conditionBody, List<CompileTestUtil.Source> extra) {
        List<CompileTestUtil.Source> sources = new ArrayList<>();
        sources.add(new CompileTestUtil.Source("demo.User", USER));
        sources.add(new CompileTestUtil.Source("demo.Dept", DEPT));
        sources.add(new CompileTestUtil.Source("demo.Role", ROLE));
        if (extra != null) {
            sources.addAll(extra);
        }
        sources.add(new CompileTestUtil.Source("demo.Cond", bind(conditionBody, "User.class")));
        return CompileTestUtil.compile(sources);
    }

    private static String bind(String fields, String entity) {
        return "import io.github.morphling.jpa.annotation.*;\n" +
                "import java.util.*;\n" +
                "@EntityCondition(entity = " + entity + ")\n" +
                "public class Cond {\n" +
                fields +
                "}\n";
    }

    @Test
    void validCondition_shouldCompile() {
        CompileTestUtil.Result r = compileCondition(
                "    @Select(type = SelectTypeEnum.LIKE) private String name;\n" +
                "    @Select(value = \"dept.name\", type = SelectTypeEnum.EQ) private String deptName;\n" +
                "    @Select(value = \"age\", type = SelectTypeEnum.GTE) private Integer ageGte;\n" +
                "    @Select(value = \"roles.code\", type = SelectTypeEnum.IN) private List<String> roleCodes;\n" +
                "    @Select(value = \"age\", type = SelectTypeEnum.BETWEEN) private List<Integer> ageBetween;\n",
                null);
        assertTrue(r.success, r.allDiagnostics());
    }

    @Test
    void unknownField_shouldFailWithSuggestion() {
        CompileTestUtil.Result r = compileCondition(
                "    @Select(value = \"nam\") private String name;\n", null);
        assertFalse(r.success);
        assertTrue(r.allDiagnostics().contains("not found"), r.allDiagnostics());
        assertTrue(r.allDiagnostics().contains("did you mean"), r.allDiagnostics());
    }

    @Test
    void unknownIntermediate_shouldFail() {
        CompileTestUtil.Result r = compileCondition(
                "    @Select(value = \"dept.name.age\") private String name;\n", null);
        assertFalse(r.success);
        assertTrue(r.allDiagnostics().contains("not a navigable association"),
                r.allDiagnostics());
    }

    @Test
    void likeOnNonStringAttribute_shouldFail() {
        CompileTestUtil.Result r = compileCondition(
                "    @Select(value = \"age\", type = SelectTypeEnum.LIKE) private Integer ageLike;\n", null);
        assertFalse(r.success);
        assertTrue(r.allDiagnostics().contains("requires a String attribute"), r.allDiagnostics());
    }

    @Test
    void likeValueNotString_shouldFail() {
        CompileTestUtil.Result r = compileCondition(
                "    @Select(value = \"name\", type = SelectTypeEnum.LIKE) private java.util.Date nameLike;\n", null);
        assertFalse(r.success);
        assertTrue(r.allDiagnostics().contains("value must be a String"), r.allDiagnostics());
    }

    @Test
    void comparisonOnNonComparableAttribute_shouldFail() {
        CompileTestUtil.Result r = compileCondition(
                "    @Select(value = \"payload\", type = SelectTypeEnum.GT) private Object payloadGt;\n", null);
        assertFalse(r.success);
        assertTrue(r.allDiagnostics().contains("requires a Comparable attribute"), r.allDiagnostics());
    }

    @Test
    void inValueNotCollection_shouldFail() {
        CompileTestUtil.Result r = compileCondition(
                "    @Select(value = \"age\", type = SelectTypeEnum.IN) private Integer ageIn;\n", null);
        assertFalse(r.success);
        assertTrue(r.allDiagnostics().contains("must be a Collection or array"), r.allDiagnostics());
    }

    @Test
    void inElementIncompatible_shouldFail() {
        CompileTestUtil.Result r = compileCondition(
                "    @Select(value = \"age\", type = SelectTypeEnum.IN) private List<String> ageIn;\n", null);
        assertFalse(r.success);
        assertTrue(r.allDiagnostics().contains("element type"), r.allDiagnostics());
        assertTrue(r.allDiagnostics().contains("not compatible"), r.allDiagnostics());
    }

    @Test
    void betweenElementIncompatible_shouldFail() {
        CompileTestUtil.Result r = compileCondition(
                "    @Select(value = \"age\", type = SelectTypeEnum.BETWEEN) private List<String> ageBetween;\n", null);
        assertFalse(r.success);
        assertTrue(r.allDiagnostics().contains("BETWEEN"), r.allDiagnostics());
        assertTrue(r.allDiagnostics().contains("not compatible"), r.allDiagnostics());
    }

    @Test
    void eqIncompatible_shouldFail() {
        CompileTestUtil.Result r = compileCondition(
                "    @Select(value = \"age\", type = SelectTypeEnum.EQ) private String ageEq;\n", null);
        assertFalse(r.success);
        assertTrue(r.allDiagnostics().contains("not compatible"), r.allDiagnostics());
    }

    @Test
    void joinTypeLengthMismatch_shouldFail() {
        CompileTestUtil.Result r = compileCondition(
                "    @Select(value = \"dept.name\", type = SelectTypeEnum.EQ,\n" +
                "            joinType = {javax.persistence.criteria.JoinType.LEFT, javax.persistence.criteria.JoinType.INNER})\n" +
                "    private String deptName;\n", null);
        assertFalse(r.success);
        assertTrue(r.allDiagnostics().contains("joinType length"), r.allDiagnostics());
    }

    @Test
    void nonEntityBinding_shouldFail() {
        List<CompileTestUtil.Source> sources = new ArrayList<>();
        sources.add(new CompileTestUtil.Source("demo.User", USER));
        sources.add(new CompileTestUtil.Source("demo.Plain", "public class Plain {\n    public String name;\n}\n"));
        sources.add(new CompileTestUtil.Source("demo.Cond",
                bind("    @Select private String name;\n", "Plain.class")));
        CompileTestUtil.Result r = CompileTestUtil.compile(sources);
        assertFalse(r.success);
        assertTrue(r.allDiagnostics().contains("not annotated @Entity"), r.allDiagnostics());
    }

    @Test
    void nestedGroupWithEntityBinding_shouldFail() {
        String body =
                "    @ConditionGroup(GroupTypeEnum.OR)\n" +
                "    @EntityCondition(entity = User.class)\n" +
                "    public static class Group {\n" +
                "        @Select private String name;\n" +
                "    }\n" +
                "    private Group group = new Group();\n";
        CompileTestUtil.Result r = compileCondition(body, null);
        assertFalse(r.success);
        assertTrue(r.allDiagnostics().contains("must not declare @EntityCondition"), r.allDiagnostics());
    }

    @Test
    void embeddedAndAssociationPaths_shouldCompile() {
        List<CompileTestUtil.Source> sources = new ArrayList<>();
        sources.add(new CompileTestUtil.Source("demo.Customer", CUSTOMER));
        sources.add(new CompileTestUtil.Source("demo.Address", ADDRESS));
        sources.add(new CompileTestUtil.Source("demo.Region", REGION));
        sources.add(new CompileTestUtil.Source("demo.Cond", bind(
                "    @Select(value = \"region.code\", type = SelectTypeEnum.EQ) private String regionCode;\n",
                "Customer.class")));
        CompileTestUtil.Result r = CompileTestUtil.compile(sources);
        assertTrue(r.success, r.allDiagnostics());
    }

    @Test
    void embeddedIntermediate_shouldFail() {
        List<CompileTestUtil.Source> sources = new ArrayList<>();
        sources.add(new CompileTestUtil.Source("demo.Customer", CUSTOMER));
        sources.add(new CompileTestUtil.Source("demo.Address", ADDRESS));
        sources.add(new CompileTestUtil.Source("demo.Region", REGION));
        sources.add(new CompileTestUtil.Source("demo.Cond", bind(
                "    @Select(value = \"address.city\", type = SelectTypeEnum.EQ) private String city;\n",
                "Customer.class")));
        CompileTestUtil.Result r = CompileTestUtil.compile(sources);
        assertFalse(r.success);
        assertTrue(r.allDiagnostics().contains("@Embedded"), r.allDiagnostics());
        assertTrue(r.allDiagnostics().contains("embedded paths are not supported"), r.allDiagnostics());
    }

    @Test
    void customResolver_shouldOnlyValidatePath() {
        List<CompileTestUtil.Source> sources = new ArrayList<>();
        sources.add(new CompileTestUtil.Source("demo.User", USER));
        sources.add(new CompileTestUtil.Source("demo.Dept", DEPT));
        sources.add(new CompileTestUtil.Source("demo.Role", ROLE));
        sources.add(new CompileTestUtil.Source("demo.MyResolver", RESOLVER));
        sources.add(new CompileTestUtil.Source("demo.Cond", bind(
                "    @Select(value = \"age\", resolver = MyResolver.class) private String anyValue;\n",
                "User.class")));
        CompileTestUtil.Result r = CompileTestUtil.compile(sources);
        assertTrue(r.success, r.allDiagnostics());
    }

    @Test
    void orderByValid_shouldCompile() {
        CompileTestUtil.Result r = compileCondition(
                "    @OrderBy(value = \"age\", direction = OrderDirection.DESC) private Boolean sortAge;\n" +
                "    @OrderBy(value = \"dept.name\", direction = OrderDirection.ASC, priority = 1) private Boolean sortDept;\n",
                null);
        assertTrue(r.success, r.allDiagnostics());
    }

    @Test
    void orderByUnknownField_shouldFail() {
        CompileTestUtil.Result r = compileCondition(
                "    @OrderBy(value = \"nam\") private Boolean sortNam;\n", null);
        assertFalse(r.success);
        assertTrue(r.allDiagnostics().contains("not found"), r.allDiagnostics());
    }

    @Test
    void orderByNonNavigableIntermediate_shouldFail() {
        CompileTestUtil.Result r = compileCondition(
                "    @OrderBy(value = \"name.age\") private Boolean sortNameAge;\n", null);
        assertFalse(r.success);
        assertTrue(r.allDiagnostics().contains("not a navigable association"), r.allDiagnostics());
    }

    @Test
    void orderByToManyLeaf_shouldFail() {
        CompileTestUtil.Result r = compileCondition(
                "    @OrderBy(value = \"roles\") private Boolean sortRoles;\n", null);
        assertFalse(r.success);
        assertTrue(r.allDiagnostics().contains("to-many collection attribute"), r.allDiagnostics());
    }
}
