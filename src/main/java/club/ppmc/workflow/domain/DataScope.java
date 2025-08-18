package club.ppmc.workflow.domain;

/**
 * @author cc
 * @description 菜单数据可见范围枚举
 */
public enum DataScope {
    /**
     * 全部数据，通常仅限系统管理员
     */
    ALL,

    /**
     * 仅查看与自己同部门的用户提交的数据
     */
    BY_DEPARTMENT,

    /**
     * 仅查看与自己同用户组的用户提交的数据
     */
    BY_GROUP,

    /**
     * 仅查看自己提交的数据
     */
    OWNER_ONLY
}