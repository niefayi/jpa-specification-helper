package io.github.morphling.jpa.test.condition;

import io.github.morphling.jpa.annotation.OrderBy;
import io.github.morphling.jpa.annotation.OrderDirection;

/**
 * Test condition with {@code @OrderBy} fields (applied by {@code OrderByStage}).
 */
public class UserOrderCondition {

    @OrderBy(value = "dept.name", direction = OrderDirection.ASC, priority = 0)
    private Boolean sortByDept;

    @OrderBy(value = "age", direction = OrderDirection.DESC, priority = 1)
    private Boolean sortByAge;

    @OrderBy(value = "name", direction = OrderDirection.ASC, priority = 2)
    private Boolean sortByName;

    public Boolean getSortByDept() {
        return sortByDept;
    }

    public void setSortByDept(Boolean sortByDept) {
        this.sortByDept = sortByDept;
    }

    public Boolean getSortByAge() {
        return sortByAge;
    }

    public void setSortByAge(Boolean sortByAge) {
        this.sortByAge = sortByAge;
    }

    public Boolean getSortByName() {
        return sortByName;
    }

    public void setSortByName(Boolean sortByName) {
        this.sortByName = sortByName;
    }
}
