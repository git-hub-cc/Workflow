package club.ppmc.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author cc
 * @description 【新增】业务API示例 - 商品数据传输对象 (DTO)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {
    private String id;
    private String name;
    private String description;
    private BigDecimal price;
    private String mainImageUrl;
    private List<String> galleryImageUrls;
    private String category;
}