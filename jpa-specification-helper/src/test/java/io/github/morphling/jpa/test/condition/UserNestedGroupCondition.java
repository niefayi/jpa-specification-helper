package io.github.morphling.jpa.test.condition;

import io.github.morphling.jpa.annotation.GroupTypeEnum;
import io.github.morphling.jpa.annotation.SelectTypeEnum;
import io.github.morphling.jpa.annotation.ConditionGroup;
import io.github.morphling.jpa.annotation.Select;

/**
 * Nested group condition: (age >= x OR age <= y) AND (name = z).
 */
public class UserNestedGroupCondition {

    @Select(value = "name")
    private String name;

    @ConditionGroup(GroupTypeEnum.OR)
    public static class AgeRange {

        @Select(value = "age", type = SelectTypeEnum.GTE)
        private Integer min;

        @Select(value = "age", type = SelectTypeEnum.LTE)
        private Integer max;

        public Integer getMin() {
            return min;
        }

        public void setMin(Integer min) {
            this.min = min;
        }

        public Integer getMax() {
            return max;
        }

        public void setMax(Integer max) {
            this.max = max;
        }
    }

    private AgeRange ageRange = new AgeRange();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AgeRange getAgeRange() {
        return ageRange;
    }

    public void setAgeRange(AgeRange ageRange) {
        this.ageRange = ageRange;
    }
}
