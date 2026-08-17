package io.github.morphling.jpa.test.condition;

import io.github.morphling.jpa.annotation.Select;

/**
 * Test condition using a custom {@link SelectPredicateResolver} referenced by
 * {@code @Select.resolver()}.
 */
public class UserCustomResolverCondition {

    @Select(value = "name", resolver = NoOpResolver.class)
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
