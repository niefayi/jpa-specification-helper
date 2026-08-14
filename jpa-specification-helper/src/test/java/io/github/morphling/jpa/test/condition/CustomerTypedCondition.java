package io.github.morphling.jpa.test.condition;

import io.github.morphling.jpa.annotation.EntityCondition;
import io.github.morphling.jpa.annotation.Select;
import io.github.morphling.jpa.annotation.SelectTypeEnum;
import io.github.morphling.jpa.test.entity.Customer;
import io.github.morphling.jpa.test.entity.CustomerFields;

/**
 * Type-safe condition bound to {@link Customer}; the {@code CustomerFields.}
 * constants are generated at compile time, so paths auto-complete in the IDE.
 */
@EntityCondition(entity = Customer.class)
public class CustomerTypedCondition {

    @Select(value = CustomerFields.name, type = SelectTypeEnum.EQ)
    private String name;

    @Select(value = CustomerFields.region.code, type = SelectTypeEnum.EQ)
    private String regionCode;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegionCode() {
        return regionCode;
    }

    public void setRegionCode(String regionCode) {
        this.regionCode = regionCode;
    }
}
