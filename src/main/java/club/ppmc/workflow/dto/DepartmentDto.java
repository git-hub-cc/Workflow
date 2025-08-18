package club.ppmc.workflow.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import java.util.List;

/**
 * @author cc
 * @description 新增的部门数据传输对象
 */
@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DepartmentDto {

    private Long id;
    private String name;
    private Long parentId;
    private String managerId;
    private Integer orderNum;
    private List<DepartmentDto> children;
}