package club.ppmc.workflow.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 你的名字
 * @description 通用的树形节点数据传输对象, 用于前端 a-tree-select 组件
 */
@Data
// 如果 children 列表为空或null, 则不序列化该字段, 节省传输体积
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TreeNodeDto {

    // 显示的文本
    private String title;

    // 唯一标识, 也是提交的值
    private String value;

    // 前端组件需要的 key, 通常与 value 相同
    private String key;

    // 子节点列表
    private List<TreeNodeDto> children = new ArrayList<>();
}