package club.ppmc.workflow.service;

import club.ppmc.workflow.dto.ComponentMetaDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @author cc
 * @description 【新增】提供组件元数据的服务
 * 在真实项目中，这些元数据可能存储在数据库中，并提供一个管理界面来动态增删组件。
 * 在此示例中，我们使用硬编码的方式来定义可用的组件。
 */
@Service
public class ComponentMetaService {

    public List<ComponentMetaDto> getComponentMetas() {
        return List.of(
                // 布局组件
                new ComponentMetaDto(
                        "container", "容器", "BorderOutlined", "布局组件",
                        Map.of("style", Map.of("padding", "20px"))
                ),
                new ComponentMetaDto(
                        "grid-row", "栅格行", "LayoutOutlined", "布局组件",
                        Map.of("gutter", 16)
                ),
                // 基础组件
                new ComponentMetaDto(
                        "rich-text", "富文本", "FontSizeOutlined", "基础组件",
                        Map.of("content", "<p>在这里输入您的文本内容...</p>")
                ),
                new ComponentMetaDto(
                        "image-box", "图片", "FileImageOutlined", "基础组件",
                        Map.of("src", "https://via.placeholder.com/400x200", "alt", "占位图")
                ),
                new ComponentMetaDto(
                        "button-link", "按钮链接", "LinkOutlined", "基础组件",
                        Map.of("text", "点击跳转", "href", "#", "type", "primary")
                ),
                // 业务组件
                new ComponentMetaDto(
                        "hero-banner", "英雄横幅", "PictureOutlined", "业务组件",
                        Map.of(
                                "title", "这是一个英雄横幅标题",
                                "subtitle", "这里是副标题，可以介绍更多信息。",
                                "imageUrl", "/images/mock/hero.jpg",
                                "buttonText", "了解更多",
                                "buttonLink", "/about"
                        )
                ),
                new ComponentMetaDto(
                        "product-grid", "商品网格", "AppstoreOutlined", "业务组件",
                        Map.of(
                                "title", "热门商品",
                                "category", "电子产品", // 默认显示"电子产品"分类
                                "count", 4 // 默认显示4个商品
                        )
                ),
                new ComponentMetaDto(
                        "article-card", "文章卡片", "ReadOutlined", "业务组件",
                        Map.of(
                                "title", "文章标题绑定",
                                "description", "文章摘要绑定",
                                "coverImage", "封面图绑定",
                                "author", "作者绑定"
                        )
                )
        );
    }
}