package club.ppmc.workflow.integration.erp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author cc
 * @description 【新增】库存扣减请求的数据传输对象 (DTO)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryDeductionRequest {
    private String sku;
    private int quantity;
}