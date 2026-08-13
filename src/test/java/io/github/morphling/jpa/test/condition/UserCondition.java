package io.github.morphling.jpa.test.condition;

import io.github.morphling.jpa.annotation.SelectTypeEnum;
import io.github.morphling.jpa.annotation.Select;
import io.github.morphling.jpa.extension.AroundDayResolver;
import io.github.morphling.jpa.extension.AroundMonthResolver;

import java.util.Date;
import java.util.List;

/**
 * Test condition object covering every {@code SelectTypeEnum}.
 */
public class UserCondition {

    @Select(type = SelectTypeEnum.EQ)
    private String name;

    @Select(value = "name", type = SelectTypeEnum.LIKE)
    private String nameLike;

    @Select(value = "name", type = SelectTypeEnum.R_LIKE)
    private String nameStart;

    @Select(value = "name", type = SelectTypeEnum.L_LIKE)
    private String nameEnd;

    @Select(value = "status", type = SelectTypeEnum.NE)
    private Integer statusNot;

    @Select(value = "age", type = SelectTypeEnum.GT)
    private Integer ageGt;

    @Select(value = "age", type = SelectTypeEnum.GTE)
    private Integer ageGte;

    @Select(value = "age", type = SelectTypeEnum.LT)
    private Integer ageLt;

    @Select(value = "age", type = SelectTypeEnum.LTE)
    private Integer ageLte;

    @Select(value = "id", type = SelectTypeEnum.IN)
    private List<Long> ids;

    @Select(value = "id", type = SelectTypeEnum.NOT_IN)
    private List<Long> notIds;

    @Select(value = "age", type = SelectTypeEnum.BETWEEN)
    private List<Integer> ageBetween;

    @Select(value = "email", type = SelectTypeEnum.IS_NULL)
    private String emailNull;

    @Select(value = "email", type = SelectTypeEnum.NOT_NULL)
    private String emailNotNull;

    @Select(value = "birthday", resolver = AroundDayResolver.class)
    private Date birthdayDay;

    @Select(value = "birthday", resolver = AroundMonthResolver.class)
    private Date birthdayMonth;

    private String ignored;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNameLike() {
        return nameLike;
    }

    public void setNameLike(String nameLike) {
        this.nameLike = nameLike;
    }

    public String getNameStart() {
        return nameStart;
    }

    public void setNameStart(String nameStart) {
        this.nameStart = nameStart;
    }

    public String getNameEnd() {
        return nameEnd;
    }

    public void setNameEnd(String nameEnd) {
        this.nameEnd = nameEnd;
    }

    public Integer getStatusNot() {
        return statusNot;
    }

    public void setStatusNot(Integer statusNot) {
        this.statusNot = statusNot;
    }

    public Integer getAgeGt() {
        return ageGt;
    }

    public void setAgeGt(Integer ageGt) {
        this.ageGt = ageGt;
    }

    public Integer getAgeGte() {
        return ageGte;
    }

    public void setAgeGte(Integer ageGte) {
        this.ageGte = ageGte;
    }

    public Integer getAgeLt() {
        return ageLt;
    }

    public void setAgeLt(Integer ageLt) {
        this.ageLt = ageLt;
    }

    public Integer getAgeLte() {
        return ageLte;
    }

    public void setAgeLte(Integer ageLte) {
        this.ageLte = ageLte;
    }

    public List<Long> getIds() {
        return ids;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids;
    }

    public List<Long> getNotIds() {
        return notIds;
    }

    public void setNotIds(List<Long> notIds) {
        this.notIds = notIds;
    }

    public List<Integer> getAgeBetween() {
        return ageBetween;
    }

    public void setAgeBetween(List<Integer> ageBetween) {
        this.ageBetween = ageBetween;
    }

    public String getEmailNull() {
        return emailNull;
    }

    public void setEmailNull(String emailNull) {
        this.emailNull = emailNull;
    }

    public String getEmailNotNull() {
        return emailNotNull;
    }

    public void setEmailNotNull(String emailNotNull) {
        this.emailNotNull = emailNotNull;
    }

    public Date getBirthdayDay() {
        return birthdayDay;
    }

    public void setBirthdayDay(Date birthdayDay) {
        this.birthdayDay = birthdayDay;
    }

    public Date getBirthdayMonth() {
        return birthdayMonth;
    }

    public void setBirthdayMonth(Date birthdayMonth) {
        this.birthdayMonth = birthdayMonth;
    }

    public String getIgnored() {
        return ignored;
    }

    public void setIgnored(String ignored) {
        this.ignored = ignored;
    }
}
