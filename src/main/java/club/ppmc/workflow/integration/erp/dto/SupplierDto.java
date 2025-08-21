package club.ppmc.workflow.integration.erp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author cc
 * @description 【新增】供应商数据的数据传输对象 (DTO)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupplierDto {
    private String id;
    private String name;
}