package io.github.anyifei12138.jpa.test.condition;

import io.github.anyifei12138.jpa.annotation.SelectTypeEnum;
import io.github.anyifei12138.jpa.annotation.Select;

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
