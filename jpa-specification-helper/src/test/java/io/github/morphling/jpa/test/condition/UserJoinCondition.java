package io.github.morphling.jpa.test.condition;

import io.github.morphling.jpa.annotation.SelectTypeEnum;
import io.github.morphling.jpa.annotation.Select;

import javax.persistence.criteria.JoinType;

/**
 * Join query condition: filters via user.dept / user.profile / user.roles.
 */
public class UserJoinCondition {

    @Select(type = SelectTypeEnum.EQ)
    private String name;

    @Select(value = "dept.name", type = SelectTypeEnum.EQ)
    private String deptName;

    @Select(value = "dept.name", type = SelectTypeEnum.LIKE)
    private String deptNameLike;

    @Select(value = "profile.nickname", type = SelectTypeEnum.EQ)
    private String profileNickname;

    @Select(value = "roles.code", type = SelectTypeEnum.EQ)
    private String roleCode;

    @Select(value = "dept.users.name", type = SelectTypeEnum.EQ)
    private String deptUserName;

    @Select(value = "dept.users.name", type = SelectTypeEnum.EQ, joinType = {JoinType.LEFT, JoinType.LEFT})
    private String deptUserNameInner;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public String getDeptNameLike() {
        return deptNameLike;
    }

    public void setDeptNameLike(String deptNameLike) {
        this.deptNameLike = deptNameLike;
    }

    public String getProfileNickname() {
        return profileNickname;
    }

    public void setProfileNickname(String profileNickname) {
        this.profileNickname = profileNickname;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getDeptUserName() {
        return deptUserName;
    }

    public void setDeptUserName(String deptUserName) {
        this.deptUserName = deptUserName;
    }

    public String getDeptUserNameInner() {
        return deptUserNameInner;
    }

    public void setDeptUserNameInner(String deptUserNameInner) {
        this.deptUserNameInner = deptUserNameInner;
    }
}
