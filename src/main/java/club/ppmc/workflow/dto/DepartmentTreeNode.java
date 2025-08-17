package club.ppmc.workflow.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * @author cc
 * @description 用于前端组织架构树的数据传输对象
 */
@Data
// 使用 equals 和 hashCode 时只比较 key，防止在构建树时因 children 不同而导致问题
@EqualsAndHashCode(of = "key")
@JsonInclude(JsonInclude.Include.NON_EMPTY) // 如果 children 为空，则不序列化
public class DepartmentTreeNode {
    // 节点标题，显示在树上
    private String title;
    // 节点的唯一标识，对于部门是 "dept_id"，对于用户是 "user_id"
    private String key;
    // 节点的值，对于部门是部门ID，对于用户是用户ID
    private String value;
    // 节点类型, 'department' 或 'user'
    private String type;
    // 是否为叶子节点（即用户节点）
    private boolean isLeaf;
    // 子节点列表
    private List<DepartmentTreeNode> children = new ArrayList<>();
}