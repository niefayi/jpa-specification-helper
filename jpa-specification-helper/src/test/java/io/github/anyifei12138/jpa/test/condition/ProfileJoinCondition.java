package io.github.anyifei12138.jpa.test.condition;

import io.github.anyifei12138.jpa.annotation.SelectTypeEnum;
import io.github.anyifei12138.jpa.annotation.Select;

/**
 * Profile-rooted join condition: filters via profile.user (one-to-one, mappedBy inverse side).
 */
public class ProfileJoinCondition {

    @Select(value = "user.name", type = SelectTypeEnum.EQ)
    private String userName;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
