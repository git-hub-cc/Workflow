package club.ppmc.workflow.domain;

/**
 * @author cc
 * @description 用户状态枚举
 */
public enum UserStatus {
    /**
     * 活动状态，可以正常登录和使用系统
     */
    ACTIVE,

    /**
     * 禁用状态，通常用于员工离职等情况，无法登录
     */
    INACTIVE,

    /**
     * 锁定状态，例如多次密码错误后，暂时无法登录
     */
    LOCKED
}