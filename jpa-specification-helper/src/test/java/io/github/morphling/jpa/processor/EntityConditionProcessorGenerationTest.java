package io.github.morphling.jpa.processor;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Generated {@code <Entity>Fields} constants tests for {@link EntityConditionProcessor}.
 */
class EntityConditionProcessorGenerationTest {

    private static final String USER =
            "import javax.persistence.*;\n" +
            "import java.util.*;\n" +
            "@Entity public class User {\n" +
            "    @Id private Long id;\n" +
            "    private String name;\n" +
            "    private Integer age;\n" +
            "    @ManyToOne private Dept dept;\n" +
            "    @OneToOne private Profile profile;\n" +
            "    @ManyToMany private List<Role> roles;\n" +
            "}\n";

    private static final String PROFILE =
            "import javax.persistence.*;\n" +
            "@Entity public class Profile {\n" +
            "    @Id private Long id;\n" +
            "    private String nickname;\n" +
            "    @OneToOne(mappedBy = \"profile\") private User user;\n" +
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
            "import java.util.*;\n" +
            "@Entity public class Role {\n" +
            "    @Id private Long id;\n" +
            "    private String code;\n" +
            "    private String name;\n" +
            "    @ManyToMany(mappedBy = \"roles\") private List<User> users;\n" +
            "}\n";

    private static final String CUSTOMER =
            "import javax.persistence.*;\n" +
            "@Entity public class Customer {\n" +
            "    @Id private Long id;\n" +
            "    private String name;\n" +
            "    @Embedded private Address address;\n" +
            "    @ManyToOne private Region region;\n" +
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

    private static String condition(String entity, String fields) {
        return "import io.github.morphling.jpa.annotation.*;\n" +
                "import java.util.*;\n" +
                "@EntityCondition(entity = " + entity + ".class)\n" +
                "public class Cond {\n" +
                fields +
                "}\n";
    }

    @Test
    void generatesUserFieldsWithAssociationsAndCycleGuards() {
        List<CompileTestUtil.Source> sources = new ArrayList<>();
        sources.add(new CompileTestUtil.Source("demo.User", USER));
        sources.add(new CompileTestUtil.Source("demo.Dept", DEPT));
        sources.add(new CompileTestUtil.Source("demo.Profile", PROFILE));
        sources.add(new CompileTestUtil.Source("demo.Role", ROLE));
        sources.add(new CompileTestUtil.Source("demo.Cond", condition("User",
                "    @Select(value = UserFields.dept.name, type = SelectTypeEnum.EQ) private String deptName;\n")));

        CompileTestUtil.Result r = CompileTestUtil.compile(sources);
        assertTrue(r.success, r.allDiagnostics());

        String generated = generated("demo.UserFields", r.generatedSources);
        assertTrue(generated.contains("String id = \"id\";"), generated);
        assertTrue(generated.contains("String name = \"name\";"), generated);
        assertTrue(generated.contains("String age = \"age\";"), generated);
        assertTrue(generated.contains("interface dept {"), generated);
        assertTrue(generated.contains("String name = \"dept.name\";"), generated);
        assertTrue(generated.contains("String users = \"dept.users\";"), generated);
        assertTrue(generated.contains("interface roles {"), generated);
        assertTrue(generated.contains("String code = \"roles.code\";"), generated);
        assertTrue(generated.contains("String users = \"roles.users\";"), generated);
    }

    @Test
    void generatesDeepAssociationPathConstants() {
        List<CompileTestUtil.Source> sources = new ArrayList<>();
        sources.add(new CompileTestUtil.Source("demo.User", USER));
        sources.add(new CompileTestUtil.Source("demo.Dept", DEPT));
        sources.add(new CompileTestUtil.Source("demo.Profile", PROFILE));
        sources.add(new CompileTestUtil.Source("demo.Role", ROLE));
        sources.add(new CompileTestUtil.Source("demo.Cond", condition("Dept",
                "    @Select(value = DeptFields.users.profile.nickname, type = SelectTypeEnum.EQ) private String nick;\n")));

        CompileTestUtil.Result r = CompileTestUtil.compile(sources);
        assertTrue(r.success, r.allDiagnostics());

        String generated = generated("demo.DeptFields", r.generatedSources);
        assertTrue(generated.contains("interface users {"), generated);
        assertTrue(generated.contains("interface profile {"), generated);
        assertTrue(generated.contains("String nickname = \"users.profile.nickname\";"), generated);
    }

    @Test
    void generatesAssociationNestedConstantsButSkipsEmbedded() {
        List<CompileTestUtil.Source> sources = new ArrayList<>();
        sources.add(new CompileTestUtil.Source("demo.Customer", CUSTOMER));
        sources.add(new CompileTestUtil.Source("demo.Address", ADDRESS));
        sources.add(new CompileTestUtil.Source("demo.Region", REGION));
        sources.add(new CompileTestUtil.Source("demo.Cond", condition("Customer",
                "    @Select(value = \"region.code\") private String regionCode;\n")));

        CompileTestUtil.Result r = CompileTestUtil.compile(sources);
        assertTrue(r.success, r.allDiagnostics());

        String generated = generated("demo.CustomerFields", r.generatedSources);
        assertTrue(generated.contains("String name = \"name\";"), generated);
        assertTrue(generated.contains("interface region {"), generated);
        assertTrue(generated.contains("String code = \"region.code\";"), generated);
        assertTrue(!generated.contains("address"), "embedded attributes must not be emitted as constants: " + generated);
    }

    private static String generated(String fqn, Map<String, String> generatedSources) {
        String content = generatedSources.get(fqn);
        assertNotNull(content, "expected generated type " + fqn + ", got " + generatedSources.keySet());
        return content;
    }
}
