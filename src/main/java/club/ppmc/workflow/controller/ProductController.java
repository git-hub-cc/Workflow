package club.ppmc.workflow.controller;

import club.ppmc.workflow.dto.ProductDto;
import club.ppmc.workflow.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author cc
 * @description 【新增】业务API示例 - 商品控制器
 * 提供了获取商品列表和商品详情的公开API，供前端渲染端调用。
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductService productService;

    /**
     * (公开) 获取商品列表，支持按分类筛选
     * @param category 可选的分类参数
     * @return 商品DTO列表
     */
    @GetMapping
    public ResponseEntity<List<ProductDto>> getProducts(@RequestParam(required = false) String category) {
        return ResponseEntity.ok(productService.getAllProducts(category));
    }

    /**
     * (公开) 根据ID获取单个商品详情
     * @param id 商品ID
     * @return 商品DTO
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable String id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    // 在实际应用中，您可以在这里添加需要管理员权限的 POST, PUT, DELETE 方法
    // 例如:
    // @PostMapping
    // @PreAuthorize("hasRole('ADMIN')")
    // public ResponseEntity<ProductDto> createProduct(@RequestBody ProductDto productDto) { ... }
}