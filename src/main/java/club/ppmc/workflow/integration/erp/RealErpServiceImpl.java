package club.ppmc.workflow.integration.erp;

import club.ppmc.workflow.integration.erp.dto.InventoryDeductionRequest;
import club.ppmc.workflow.integration.erp.dto.SupplierDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

/**
 * @author cc
 * @description 【新增】ErpService 的真实实现 (仅在 prod 环境下激活)
 * <p>
 * 这个实现类用于生产环境。它会读取配置文件中的真实ERP API地址，
 * 并使用 RestTemplate (或 WebClient) 发起真实的 HTTP 请求。
 */
@Service
@Profile("prod") // 关键注解：指定此服务仅在 "prod" profile 激活时生效
@Slf4j
@RequiredArgsConstructor
public class RealErpServiceImpl implements ErpService {

    // 从 application-prod.properties 注入真实的ERP API基地址
    @Value("${erp.api.base-url}")
    private String erpApiBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate(); // 在实际项目中, RestTemplate 应作为Bean注入

    @Override
    public List<SupplierDto> getSuppliers() {
        String url = erpApiBaseUrl + "/suppliers";
        log.info(">>> [REAL] 调用真实ERP接口获取供应商: GET {}", url);
        try {
            // 这里是真实调用的示例
            SupplierDto[] suppliers = restTemplate.getForObject(url, SupplierDto[].class);
            return suppliers != null ? Arrays.asList(suppliers) : List.of();
        } catch (Exception e) {
            log.error("调用真实ERP供应商接口失败", e);
            // 在真实项目中，这里应该有更完善的异常处理机制
            throw new RuntimeException("无法从ERP系统获取供应商数据", e);
        }
    }

    @Override
    public void deductInventory(InventoryDeductionRequest request) {
        String url = erpApiBaseUrl + "/inventory/deduct";
        log.info(">>> [REAL] 调用真实ERP接口扣减库存: POST {}", url);
        try {
            // 这里是真实调用的示例
            restTemplate.postForEntity(url, request, Void.class);
            log.info(">>> [REAL] 库存扣减请求已成功发送至ERP: SKU = {}, 数量 = {}", request.getSku(), request.getQuantity());
        } catch (Exception e) {
            log.error("调用真实ERP库存扣减接口失败", e);
            // 在真实项目中，这里可能需要触发重试机制或发送警报
            throw new RuntimeException("无法通知ERP系统扣减库存", e);
        }
    }
}