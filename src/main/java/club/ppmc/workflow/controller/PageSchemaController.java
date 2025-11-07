package club.ppmc.workflow.controller;

import club.ppmc.workflow.dto.PageSchemaDto;
import club.ppmc.workflow.service.PageSchemaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * @author cc
 * @description 【新增】管理低代码页面结构的 RESTful API 控制器
 */
@RestController
// 【权限修复】将所有页面管理接口移至 /api/admin 路径下，统一由 SecurityConfig 保护
@RequestMapping("/api/admin/schemas")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
// 【权限修复】在类级别添加权限注解，确保此控制器下所有接口都需要管理员权限
@PreAuthorize("hasRole('ADMIN')")
public class PageSchemaController {

    private final PageSchemaService pageSchemaService;

    // 【权限修复】此公开接口已移至 PublicController.java
    /*
    @GetMapping("/{pageKey}")
    public ResponseEntity<PageSchemaDto> getPageSchemaByKey(@PathVariable String pageKey) {
        return ResponseEntity.ok(pageSchemaService.getPageSchemaByKey(pageKey));
    }
    */

    /**
     * 【权限修复】新增一个按ID获取页面结构的接口，供页面设计器的编辑模式使用
     *
     * @param id 页面结构的数据库ID
     * @return PageSchemaDto
     */
    @GetMapping("/{id}")
    public ResponseEntity<PageSchemaDto> getSchemaById(@PathVariable Long id) {
        return ResponseEntity.ok(pageSchemaService.getSchemaById(id));
    }


    /**
     * (管理员) 分页获取所有页面结构列表。
     */
    @GetMapping
    public ResponseEntity<Page<PageSchemaDto>> getAllSchemas(Pageable pageable) {
        return ResponseEntity.ok(pageSchemaService.getAllSchemas(pageable));
    }

    /**
     * (管理员) 创建一个新的页面结构。
     */
    @PostMapping
    public ResponseEntity<PageSchemaDto> createSchema(@RequestBody PageSchemaDto dto) {
        PageSchemaDto createdDto = pageSchemaService.createSchema(dto);
        return new ResponseEntity<>(createdDto, HttpStatus.CREATED);
    }

    /**
     * (管理员) 更新一个已有的页面结构。
     */
    @PutMapping("/{id}")
    public ResponseEntity<PageSchemaDto> updateSchema(@PathVariable Long id, @RequestBody PageSchemaDto dto) {
        return ResponseEntity.ok(pageSchemaService.updateSchema(id, dto));
    }

    /**
     * (管理员) 删除一个页面结构。
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchema(@PathVariable Long id) {
        pageSchemaService.deleteSchema(id);
        return ResponseEntity.noContent().build();
    }
}