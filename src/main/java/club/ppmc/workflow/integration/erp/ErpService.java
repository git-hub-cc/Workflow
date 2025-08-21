package club.ppmc.workflow.integration.erp;

import club.ppmc.workflow.integration.erp.dto.InventoryDeductionRequest;
import club.ppmc.workflow.integration.erp.dto.SupplierDto;

import java.util.List;

/**
 * @author cc
 * @description 【新增】ERP 系统交互服务接口 (契约)
 * <p>
 * 定义了与ERP系统进行数据交互的所有方法。
 * 这是实现后端服务与具体ERP实现解耦的关键。
 * 业务逻辑层（如 Controller 或流程 Delegate）应始终依赖此接口，而不是具体的实现类。
 */
public interface ErpService {

    /**
     * 从ERP获取供应商列表
     * @return 供应商数据传输对象列表
     */
    List<SupplierDto> getSuppliers();

    /**
     * 通知ERP进行库存扣减
     * @param request 包含SKU和数量的请求对象
     */
    void deductInventory(InventoryDeductionRequest request);

}