package club.ppmc.workflow.service;

import club.ppmc.workflow.domain.PageSchema;
import club.ppmc.workflow.dto.PageSchemaDto;
import club.ppmc.workflow.exception.ResourceNotFoundException;
import club.ppmc.workflow.repository.PageSchemaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author cc
 * @description 【新增】封装 PageSchema 业务逻辑的服务
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PageSchemaService {

    private final PageSchemaRepository pageSchemaRepository;

    /**
     * (供渲染端调用) 根据 pageKey 获取单个页面结构
     *
     * @param pageKey 页面的唯一标识
     * @return PageSchemaDto
     */
    @Transactional(readOnly = true)
    public PageSchemaDto getPageSchemaByKey(String pageKey) {
        PageSchema schema = pageSchemaRepository.findByPageKey(pageKey)
                .orElseThrow(() -> new ResourceNotFoundException("未找到页面: " + pageKey));
        return toDto(schema);
    }

    /**
     * 【权限修复】新增一个按ID获取页面结构的方法，供 PageSchemaController 使用
     *
     * @param id 页面结构的数据库ID
     * @return PageSchemaDto
     */
    @Transactional(readOnly = true)
    public PageSchemaDto getSchemaById(Long id) {
        PageSchema schema = pageSchemaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("未找到页面 ID: " + id));
        return toDto(schema);
    }

    /**
     * (供管理端调用) 分页获取所有页面结构列表
     *
     * @param pageable 分页参数
     * @return Page<PageSchemaDto>
     */
    @Transactional(readOnly = true)
    public Page<PageSchemaDto> getAllSchemas(Pageable pageable) {
        return pageSchemaRepository.findAll(pageable).map(this::toDto);
    }

    /**
     * (供设计器调用) 创建一个新的页面结构
     *
     * @param dto 包含页面结构信息的 DTO
     * @return 创建后的 PageSchemaDto
     */
    public PageSchemaDto createSchema(PageSchemaDto dto) {
        if (pageSchemaRepository.existsByPageKey(dto.getPageKey())) {
            throw new IllegalArgumentException("页面标识 (Key) '" + dto.getPageKey() + "' 已存在。");
        }
        PageSchema schema = new PageSchema();
        schema.setPageKey(dto.getPageKey());
        schema.setName(dto.getName());
        schema.setSchemaJson(dto.getSchemaJson());
        return toDto(pageSchemaRepository.save(schema));
    }

    /**
     * (供设计器调用) 更新一个已有的页面结构
     *
     * @param id  页面结构的数据库ID
     * @param dto 包含更新信息的 DTO
     * @return 更新后的 PageSchemaDto
     */
    public PageSchemaDto updateSchema(Long id, PageSchemaDto dto) {
        PageSchema schema = pageSchemaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("未找到页面 ID: " + id));

        // 如果 pageKey 发生变化，需要检查新 key 是否已存在
        if (!schema.getPageKey().equals(dto.getPageKey()) && pageSchemaRepository.existsByPageKey(dto.getPageKey())) {
            throw new IllegalArgumentException("页面标识 (Key) '" + dto.getPageKey() + "' 已存在。");
        }

        schema.setPageKey(dto.getPageKey());
        schema.setName(dto.getName());
        schema.setSchemaJson(dto.getSchemaJson());
        return toDto(pageSchemaRepository.save(schema));
    }

    /**
     * (供管理端调用) 删除一个页面结构
     *
     * @param id 页面结构的数据库ID
     */
    public void deleteSchema(Long id) {
        if (!pageSchemaRepository.existsById(id)) {
            throw new ResourceNotFoundException("未找到要删除的页面 ID: " + id);
        }
        pageSchemaRepository.deleteById(id);
    }

    private PageSchemaDto toDto(PageSchema entity) {
        PageSchemaDto dto = new PageSchemaDto();
        dto.setId(entity.getId());
        dto.setPageKey(entity.getPageKey());
        dto.setName(entity.getName());
        dto.setSchemaJson(entity.getSchemaJson());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}