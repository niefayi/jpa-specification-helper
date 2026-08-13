package io.github.anyifei12138.jpa.test.condition;

import io.github.anyifei12138.jpa.annotation.GroupTypeEnum;
import io.github.anyifei12138.jpa.annotation.ConditionGroup;
import io.github.anyifei12138.jpa.annotation.Select;

/**
 * OR group condition: either name OR status must match.
 */
public class UserGroupCondition {

    @ConditionGroup(GroupTypeEnum.OR)
    public static class NameOrStatus {

        @Select(value = "name")
        private String name;

        @Select(value = "status")
        private Integer status;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }
    }

    private NameOrStatus nameOrStatus = new NameOrStatus();

    public NameOrStatus getNameOrStatus() {
        return nameOrStatus;
    }

    public void setNameOrStatus(NameOrStatus nameOrStatus) {
        this.nameOrStatus = nameOrStatus;
    }
}
