package club.ppmc.workflow.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author cc
 * @description 自定义操作日志注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogOperation {

    /**
     * 操作模块
     */
    String module();

    /**
     * 具体操作
     */
    String action();

    /**
     * 操作对象ID的SpEL表达式, 用于从方法参数中提取ID
     * 例如: "#userDto.id"
     */
    String targetIdExpression() default "";
}