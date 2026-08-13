package io.github.morphling.jpa.test.condition;

import io.github.morphling.jpa.annotation.SelectTypeEnum;
import io.github.morphling.jpa.annotation.Select;

/**
 * Role-rooted join condition: filters via role.users (many-to-many, mappedBy inverse side).
 */
public class RoleJoinCondition {

    @Select(value = "users.name", type = SelectTypeEnum.EQ)
    private String userName;

    @Select(value = "users.dept.name", type = SelectTypeEnum.EQ)
    private String userDeptName;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserDeptName() {
        return userDeptName;
    }

    public void setUserDeptName(String userDeptName) {
        this.userDeptName = userDeptName;
    }
}
