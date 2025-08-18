package club.ppmc.workflow.dto;

import club.ppmc.workflow.domain.Menu;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

/**
 * @author cc
 * @description 菜单数据传输对象，用于构建前端树形结构
 */
@Data
// 如果字段为 null 或 children 列表为空，则不序列化该字段, 节省传输体积
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class MenuDto {
    private Long id;
    private Long parentId;
    private String name;
    private String path;
    private String icon;
    private Menu.MenuType type;
    private String componentPath;
    private Long formDefinitionId;
    private Integer orderNum;
    private boolean visible;
    private List<String> roleNames;
    private List<MenuDto> children;
    // --- 【新增字段】 ---
    private String dataScope;
}