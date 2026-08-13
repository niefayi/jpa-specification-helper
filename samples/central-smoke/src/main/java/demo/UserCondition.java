package demo;

import io.github.morphling.jpa.annotation.Select;
import io.github.morphling.jpa.annotation.SelectTypeEnum;

public class UserCondition {

    @Select(type = SelectTypeEnum.LIKE)
    private String name;

    @Select(value = "age", type = SelectTypeEnum.GTE)
    private Integer ageMin;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAgeMin() {
        return ageMin;
    }

    public void setAgeMin(Integer ageMin) {
        this.ageMin = ageMin;
    }
}
