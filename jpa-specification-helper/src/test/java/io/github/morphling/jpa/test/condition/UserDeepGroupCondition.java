package io.github.morphling.jpa.test.condition;

import io.github.morphling.jpa.annotation.GroupTypeEnum;
import io.github.morphling.jpa.annotation.SelectTypeEnum;
import io.github.morphling.jpa.annotation.ConditionGroup;
import io.github.morphling.jpa.annotation.Select;

/**
 * Deeply nested group condition: name = x OR (age >= y AND status != z).
 */
public class UserDeepGroupCondition {

    @ConditionGroup(GroupTypeEnum.OR)
    public static class Outer {

        @Select(value = "name")
        private String name;

        @ConditionGroup(GroupTypeEnum.AND)
        public static class AgeStatus {

            @Select(value = "age", type = SelectTypeEnum.GTE)
            private Integer ageMin;

            @Select(value = "status", type = SelectTypeEnum.NE)
            private Integer statusNot;

            public Integer getAgeMin() {
                return ageMin;
            }

            public void setAgeMin(Integer ageMin) {
                this.ageMin = ageMin;
            }

            public Integer getStatusNot() {
                return statusNot;
            }

            public void setStatusNot(Integer statusNot) {
                this.statusNot = statusNot;
            }
        }

        private AgeStatus ageStatus = new AgeStatus();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public AgeStatus getAgeStatus() {
            return ageStatus;
        }

        public void setAgeStatus(AgeStatus ageStatus) {
            this.ageStatus = ageStatus;
        }
    }

    private Outer outer = new Outer();

    public Outer getOuter() {
        return outer;
    }

    public void setOuter(Outer outer) {
        this.outer = outer;
    }
}
