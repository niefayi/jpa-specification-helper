package io.github.morphling.jpa.test.condition;

import io.github.morphling.jpa.annotation.SelectTypeEnum;
import io.github.morphling.jpa.annotation.Select;

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
