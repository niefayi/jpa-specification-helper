package io.github.anyifei12138.jpa.test.condition;

import io.github.anyifei12138.jpa.annotation.GroupTypeEnum;
import io.github.anyifei12138.jpa.annotation.SelectTypeEnum;
import io.github.anyifei12138.jpa.annotation.ConditionGroup;
import io.github.anyifei12138.jpa.annotation.Select;

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
