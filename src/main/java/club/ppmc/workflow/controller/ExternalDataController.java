package club.ppmc.workflow.controller;

import club.ppmc.workflow.integration.erp.ErpService;
import club.ppmc.workflow.integration.erp.dto.SupplierDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author cc
 * @description 【新增】用于暴露外部系统数据的控制器
 * <p>
 * 这个控制器作为后端的一个代理，将对外部系统（如ERP）的数据请求封装成内部API。
 * 前端通过调用这里的接口来获取数据，而不是直接连接外部系统。
 * 控制器注入了 ErpService 接口，Spring会根据当前激活的Profile自动选择使用 MockErpServiceImpl 还是 RealErpServiceImpl。
 */
@RestController
@RequestMapping("/api/external")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@PreAuthorize("isAuthenticated()") // 保护接口，只有登录用户才能访问
public class ExternalDataController {

    private final ErpService erpService;

    /**
     * API: 获取供应商列表
     * @return 供应商DTO列表
     */
    @GetMapping("/suppliers")
    public ResponseEntity<List<SupplierDto>> getSuppliers() {
        return ResponseEntity.ok(erpService.getSuppliers());
    }

    // 未来可以扩展其他外部数据接口，例如:
    // @GetMapping("/products")
    // public ResponseEntity<List<ProductDto>> getProducts() { ... }
}