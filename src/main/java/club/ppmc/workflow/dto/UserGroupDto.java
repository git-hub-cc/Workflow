package club.ppmc.workflow.dto;

import lombok.Data;

/**
 * @author 你的名字
 * @description 用户组数据传输对象
 */
@Data
public class UserGroupDto {
    private Long id;
    private String name;
    private String description;
}