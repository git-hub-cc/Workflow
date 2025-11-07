package club.ppmc.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * @author cc
 * @description 【新增】前端组件元数据 DTO
 * 用于向设计器提供组件信息，以便动态生成组件面板。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComponentMetaDto {
    /**
     * 组件的唯一类型标识，例如 "hero-banner"
     */
    private String type;

    /**
     * 组件在设计器中显示的名称，例如 "英雄横幅"
     */
    private String name;

    /**
     * 组件在设计器中显示的图标 (例如 Ant Design Icon 的名称)
     */
    private String icon;

    /**
     * 组件所属的分类，用于在设计器中分组，例如 "基础组件", "布局组件", "业务组件"
     */
    private String category;

    /**
     * 组件的默认属性，当组件被拖拽到画布上时，将使用这些属性初始化。
     * 值可以是任何可以被序列化为JSON的对象。
     */
    private Map<String, Object> defaultProps;
}