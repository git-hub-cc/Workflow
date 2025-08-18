package club.ppmc.workflow.exception;

/**
 * @author cc
 * @description 【新增】当资源（如角色、用户组）因被其他实体引用而无法删除时抛出的自定义业务异常。
 */
public class ResourceInUseException extends RuntimeException {
    public ResourceInUseException(String message) {
        super(message);
    }
}