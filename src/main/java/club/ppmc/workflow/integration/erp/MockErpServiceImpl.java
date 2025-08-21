package club.ppmc.workflow.integration.erp;

import club.ppmc.workflow.integration.erp.dto.InventoryDeductionRequest;
import club.ppmc.workflow.integration.erp.dto.SupplierDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author cc
 * @description 【新增】ErpService 的模拟实现 (仅在 dev 环境下激活)
 * <p>
 * 这个实现类用于开发和测试环境。它不进行任何真实的API调用，
 * 而是返回硬编码的假数据，并/或在日志中打印操作信息。
 * 这使得前端和后端开发人员可以在没有真实ERP环境的情况下进行联调和功能验证。
 */
@Service
@Slf4j
public class MockErpServiceImpl implements ErpService {

    @Override
    public List<SupplierDto> getSuppliers() {
        log.info(">>> [MOCK] 调用 ErpService.getSuppliers(), 返回模拟数据。");
        // 返回一个硬编码的供应商列表作为模拟数据
        return List.of(
                new SupplierDto("SUP-001", "Acme 公司 (模拟)"),
                new SupplierDto("SUP-002", "全球创新科技 (模拟)"),
                new SupplierDto("SUP-003", "先锋材料供应商 (模拟)")
        );
    }

    @Override
    public void deductInventory(InventoryDeductionRequest request) {
        log.info(">>> [MOCK] 调用 ErpService.deductInventory(), 打印请求信息。");
        log.info(">>> [MOCK] 收到库存扣减请求: SKU = {}, 数量 = {}", request.getSku(), request.getQuantity());
        // 在这里不执行任何操作，仅打印日志
    }
}