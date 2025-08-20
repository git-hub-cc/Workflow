package club.ppmc.workflow.service;

import club.ppmc.workflow.domain.*;
import club.ppmc.workflow.dto.MenuDto;
import club.ppmc.workflow.exception.ResourceNotFoundException;
import club.ppmc.workflow.repository.FormDefinitionRepository;
import club.ppmc.workflow.repository.MenuRepository;
import club.ppmc.workflow.repository.RoleRepository;
import club.ppmc.workflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author cc
 * @description 菜单管理的业务逻辑服务
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MenuService {

    private final MenuRepository menuRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final FormDefinitionRepository formDefinitionRepository;

    /**
     * 【核心修复】获取指定用户的可访问菜单树
     *  - 为管理员(ADMIN)增加特殊逻辑，使其能够看到所有菜单。
     *
     * @param userId 用户ID
     * @return 菜单 DTO 树形列表
     */
    @Transactional(readOnly = true)
    public List<MenuDto> getMenuTreeForUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("未找到用户: " + userId));
        Set<Role> userRoles = user.getRoles();

        // 检查用户是否拥有 "ADMIN" 角色
        boolean isAdmin = userRoles.stream()
                .anyMatch(role -> "ADMIN".equals(role.getName()));

        List<Menu> accessibleMenus;

        if (isAdmin) {
            // 如果是管理员，获取所有菜单
            accessibleMenus = menuRepository.findAll();
            // 确保管理员看到的菜单也是排好序的
            accessibleMenus.sort(Comparator.comparing(Menu::getOrderNum, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(Menu::getName));
        } else {
            // 如果不是管理员，则按角色获取可见菜单
            if (userRoles.isEmpty()) {
                return Collections.emptyList();
            }
            accessibleMenus = menuRepository.findVisibleMenusByRoles(userRoles);
        }

        return buildMenuTree(accessibleMenus.stream().map(this::toDto).collect(Collectors.toList()));
    }


    /**
     * 获取所有菜单的完整树形结构 (供管理员使用)
     *
     * @return 所有菜单的 DTO 树形列表
     */
    @Transactional(readOnly = true)
    public List<MenuDto> getAllMenusAsTree() {
        List<Menu> allMenus = menuRepository.findAll();
        allMenus.sort(Comparator.comparing(Menu::getOrderNum, Comparator.nullsLast(Comparator.naturalOrder())));
        return buildMenuTree(allMenus.stream().map(this::toDto).collect(Collectors.toList()));
    }

    /**
     * 创建一个新菜单
     *
     * @param menuDto 菜单数据
     * @return 创建后的菜单 DTO
     */
    public MenuDto createMenu(MenuDto menuDto) {
        Menu menu = toEntity(menuDto);
        Menu savedMenu = menuRepository.save(menu);
        return toDto(savedMenu);
    }

    /**
     * 更新菜单信息
     *
     * @param id      菜单ID
     * @param menuDto 更新数据
     * @return 更新后的菜单 DTO
     */
    public MenuDto updateMenu(Long id, MenuDto menuDto) {
        Menu existingMenu = menuRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("未找到菜单ID: " + id));

        // 更新基础属性
        existingMenu.setName(menuDto.getName());
        existingMenu.setPath(menuDto.getPath());
        existingMenu.setIcon(menuDto.getIcon());
        existingMenu.setType(menuDto.getType());
        existingMenu.setComponentPath(menuDto.getComponentPath());
        existingMenu.setOrderNum(menuDto.getOrderNum());
        existingMenu.setVisible(menuDto.isVisible());
        existingMenu.setParentId(menuDto.getParentId());
        // --- 【核心修改】更新数据范围 ---
        if (StringUtils.hasText(menuDto.getDataScope())) {
            existingMenu.setDataScope(DataScope.valueOf(menuDto.getDataScope()));
        } else {
            existingMenu.setDataScope(null);
        }

        // 更新关联的表单
        if (menuDto.getFormDefinitionId() != null) {
            FormDefinition formDef = formDefinitionRepository.findById(menuDto.getFormDefinitionId())
                    .orElseThrow(() -> new ResourceNotFoundException("未找到关联的表单定义ID: " + menuDto.getFormDefinitionId()));
            existingMenu.setFormDefinition(formDef);
        } else {
            existingMenu.setFormDefinition(null);
        }

        // 更新关联的角色
        if (!CollectionUtils.isEmpty(menuDto.getRoleNames())) {
            Set<Role> roles = roleRepository.findByNameIn(menuDto.getRoleNames());
            existingMenu.setRoles(roles);
        } else {
            existingMenu.getRoles().clear();
        }

        Menu updatedMenu = menuRepository.save(existingMenu);
        return toDto(updatedMenu);
    }

    /**
     * 删除一个菜单
     *
     * @param id 菜单ID
     */
    public void deleteMenu(Long id) {
        if (!menuRepository.existsById(id)) {
            throw new ResourceNotFoundException("未找到菜单ID: " + id);
        }
        if (menuRepository.existsByParentId(id)) {
            throw new IllegalStateException("无法删除，该菜单下存在子菜单");
        }
        menuRepository.deleteById(id);
    }


    // --- Helper Methods ---

    /**
     * 将扁平的菜单列表构建成树形结构
     */
    private List<MenuDto> buildMenuTree(List<MenuDto> menus) {
        Map<Long, MenuDto> menuMap = menus.stream()
                .collect(Collectors.toMap(MenuDto::getId, Function.identity()));

        List<MenuDto> tree = new ArrayList<>();
        for (MenuDto menu : menus) {
            if (menu.getParentId() == null || menu.getParentId() == 0) {
                tree.add(menu);
            } else {
                MenuDto parent = menuMap.get(menu.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(menu);
                }
            }
        }
        return tree;
    }

    /**
     * DTO to Entity
     */
    private Menu toEntity(MenuDto dto) {
        Menu entity = new Menu();
        entity.setId(dto.getId());
        entity.setParentId(dto.getParentId());
        entity.setName(dto.getName());
        entity.setPath(dto.getPath());
        entity.setIcon(dto.getIcon());
        entity.setType(dto.getType());
        entity.setComponentPath(dto.getComponentPath());
        entity.setOrderNum(dto.getOrderNum());
        entity.setVisible(dto.isVisible());

        // --- 【核心修改】处理数据范围 ---
        if (StringUtils.hasText(dto.getDataScope())) {
            entity.setDataScope(DataScope.valueOf(dto.getDataScope()));
        }

        if (dto.getFormDefinitionId() != null) {
            FormDefinition formDef = formDefinitionRepository.findById(dto.getFormDefinitionId())
                    .orElseThrow(() -> new ResourceNotFoundException("未找到关联的表单定义ID: " + dto.getFormDefinitionId()));
            entity.setFormDefinition(formDef);
        }
        if (!CollectionUtils.isEmpty(dto.getRoleNames())) {
            entity.setRoles(roleRepository.findByNameIn(dto.getRoleNames()));
        }
        return entity;
    }

    /**
     * Entity to DTO
     */
    private MenuDto toDto(Menu entity) {
        MenuDto dto = new MenuDto();
        dto.setId(entity.getId());
        dto.setParentId(entity.getParentId());
        dto.setName(entity.getName());
        dto.setPath(entity.getPath());
        dto.setIcon(entity.getIcon());
        dto.setType(entity.getType());
        dto.setComponentPath(entity.getComponentPath());
        dto.setOrderNum(entity.getOrderNum());
        dto.setVisible(entity.isVisible());

        // --- 【核心修改】处理数据范围 ---
        if (entity.getDataScope() != null) {
            dto.setDataScope(entity.getDataScope().name());
        }

        if (entity.getFormDefinition() != null) {
            dto.setFormDefinitionId(entity.getFormDefinition().getId());
        }
        if (!CollectionUtils.isEmpty(entity.getRoles())) {
            dto.setRoleNames(entity.getRoles().stream().map(Role::getName).collect(Collectors.toList()));
        }
        return dto;
    }
}