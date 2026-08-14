package io.github.morphling.jpa.test.condition;

import io.github.morphling.jpa.annotation.EntityCondition;
import io.github.morphling.jpa.annotation.Select;
import io.github.morphling.jpa.annotation.SelectTypeEnum;
import io.github.morphling.jpa.test.entity.User;
import io.github.morphling.jpa.test.entity.UserFields;

/**
 * Type-safe condition bound to {@link User}; the {@code UserFields.} constants are
 * generated at compile time by {@code EntityConditionProcessor}, so the paths here
 * auto-complete in the IDE and are validated against the entity on every build.
 */
@EntityCondition(entity = User.class)
public class UserTypedCondition {

    @Select(value = UserFields.name, type = SelectTypeEnum.LIKE)
    private String name;

    @Select(value = UserFields.dept.name, type = SelectTypeEnum.LIKE)
    private String deptName;

    @Select(value = UserFields.age, type = SelectTypeEnum.GTE)
    private Integer ageGte;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public Integer getAgeGte() {
        return ageGte;
    }

    public void setAgeGte(Integer ageGte) {
        this.ageGte = ageGte;
    }
}
