package club.ppmc.workflow.service;

import club.ppmc.workflow.dto.ProductDto;
import club.ppmc.workflow.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author cc
 * @description 【新增】业务API示例 - 商品服务 (使用内存模拟数据)
 */
@Service
public class ProductService {

    // 使用内存中的 ConcurrentHashMap 模拟数据库
    private static final Map<String, ProductDto> MOCK_DB = new ConcurrentHashMap<>();

    // 初始化一些模拟数据
    static {
        MOCK_DB.put("p001", new ProductDto("p001", "高性能笔记本电脑", "搭载最新款处理器，16GB内存，超高速固态硬盘，专为专业人士打造。", new BigDecimal("8999.00"), "/images/mock/laptop.jpg", List.of(), "电子产品"));
        MOCK_DB.put("p002", new ProductDto("p002", "机械键盘", "提供无与伦比的打字体验，RGB背光，多种轴体可选。", new BigDecimal("699.00"), "/images/mock/keyboard.jpg", List.of(), "电脑配件"));
        MOCK_DB.put("p003", new ProductDto("p003", "人体工学办公椅", "全天候舒适支撑，保护您的脊椎健康。", new BigDecimal("1299.00"), "/images/mock/chair.jpg", List.of(), "办公家具"));
        MOCK_DB.put("p004", new ProductDto("p004", "4K高清显示器", "27英寸IPS面板，色彩精准，视觉效果震撼。", new BigDecimal("2499.00"), "/images/mock/monitor.jpg", List.of(), "电子产品"));
    }

    /**
     * 获取所有商品列表（支持简单筛选）
     * @param category 可选的分类筛选
     * @return 商品列表
     */
    public List<ProductDto> getAllProducts(String category) {
        if (category == null || category.isEmpty()) {
            return new ArrayList<>(MOCK_DB.values());
        }
        return MOCK_DB.values().stream()
                .filter(p -> category.equals(p.getCategory()))
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取单个商品
     * @param id 商品ID
     * @return 商品 DTO
     */
    public ProductDto getProductById(String id) {
        ProductDto product = MOCK_DB.get(id);
        if (product == null) {
            throw new ResourceNotFoundException("未找到商品: " + id);
        }
        return product;
    }
}