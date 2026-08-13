package io.github.morphling.jpa.test.condition;

import io.github.morphling.jpa.annotation.SelectTypeEnum;
import io.github.morphling.jpa.annotation.Select;

/**
 * Dept-rooted join condition: filters via dept.users (one-to-many, mappedBy inverse side).
 */
public class DeptJoinCondition {

    @Select(value = "users.name", type = SelectTypeEnum.EQ)
    private String userName;

    @Select(value = "users.profile.nickname", type = SelectTypeEnum.EQ)
    private String userProfileNickname;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserProfileNickname() {
        return userProfileNickname;
    }

    public void setUserProfileNickname(String userProfileNickname) {
        this.userProfileNickname = userProfileNickname;
    }
}
