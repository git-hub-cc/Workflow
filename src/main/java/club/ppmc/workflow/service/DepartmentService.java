package club.ppmc.workflow.service;

import club.ppmc.workflow.domain.Department;
import club.ppmc.workflow.domain.User;
import club.ppmc.workflow.dto.DepartmentDto;
import club.ppmc.workflow.exception.ResourceNotFoundException;
import club.ppmc.workflow.repository.DepartmentRepository;
import club.ppmc.workflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author cc
 * @description 新增的部门管理业务逻辑服务
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    /**
     * 创建新部门
     */
    public DepartmentDto createDepartment(DepartmentDto departmentDto) {
        Department department = new Department();
        department.setName(departmentDto.getName());
        department.setOrderNum(departmentDto.getOrderNum());

        if (departmentDto.getParentId() != null) {
            Department parent = departmentRepository.findById(departmentDto.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("未找到父部门 ID: " + departmentDto.getParentId()));
            department.setParent(parent);
        }

        if (StringUtils.hasText(departmentDto.getManagerId())) {
            User manager = userRepository.findById(departmentDto.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("未找到部门负责人 ID: " + departmentDto.getManagerId()));
            department.setManager(manager);
        }

        Department savedDept = departmentRepository.save(department);
        return toDto(savedDept);
    }

    /**
     * 更新部门信息
     */
    public DepartmentDto updateDepartment(Long id, DepartmentDto departmentDto) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("未找到部门 ID: " + id));

        department.setName(departmentDto.getName());
        department.setOrderNum(departmentDto.getOrderNum());

        // 更新父部门
        if (departmentDto.getParentId() != null) {
            // 防止将部门设置为自己的子部门
            if (isChildOf(department, departmentDto.getParentId())) {
                throw new IllegalArgumentException("不能将一个部门移动到它自己的子部门下。");
            }
            Department parent = departmentRepository.findById(departmentDto.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("未找到父部门 ID: " + departmentDto.getParentId()));
            department.setParent(parent);
        } else {
            department.setParent(null);
        }

        // 更新部门负责人
        if (StringUtils.hasText(departmentDto.getManagerId())) {
            User manager = userRepository.findById(departmentDto.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("未找到部门负责人 ID: " + departmentDto.getManagerId()));
            department.setManager(manager);
        } else {
            department.setManager(null);
        }

        Department updatedDept = departmentRepository.save(department);
        return toDto(updatedDept);
    }

    /**
     * 删除部门
     */
    public void deleteDepartment(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("未找到部门 ID: " + id));

        // 检查是否有子部门
        if (!department.getChildren().isEmpty()) {
            throw new IllegalStateException("无法删除，该部门下存在子部门。");
        }
        // 检查部门下是否还有员工
        if (!department.getUsers().isEmpty()) {
            throw new IllegalStateException("无法删除，该部门下仍有员工。");
        }

        departmentRepository.deleteById(id);
    }

    /**
     * 获取完整的部门树
     */
    @Transactional(readOnly = true)
    public List<DepartmentDto> getDepartmentTree() {
        List<Department> allDepartments = departmentRepository.findAll();
        List<DepartmentDto> allDtos = allDepartments.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        Map<Long, DepartmentDto> dtoMap = allDtos.stream()
                .collect(Collectors.toMap(DepartmentDto::getId, Function.identity()));

        List<DepartmentDto> tree = new ArrayList<>();
        for (DepartmentDto dto : allDtos) {
            if (dto.getParentId() == null) {
                tree.add(dto);
            } else {
                DepartmentDto parentDto = dtoMap.get(dto.getParentId());
                if (parentDto != null) {
                    if (parentDto.getChildren() == null) {
                        parentDto.setChildren(new ArrayList<>());
                    }
                    parentDto.getChildren().add(dto);
                }
            }
        }

        // 对树进行排序
        sortTree(tree);
        return tree;
    }

    // --- 辅助方法 ---

    private DepartmentDto toDto(Department entity) {
        DepartmentDto dto = new DepartmentDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setOrderNum(entity.getOrderNum());
        if (entity.getParent() != null) {
            dto.setParentId(entity.getParent().getId());
        }
        if (entity.getManager() != null) {
            dto.setManagerId(entity.getManager().getId());
        }
        return dto;
    }

    private void sortTree(List<DepartmentDto> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        nodes.sort(Comparator.comparing(DepartmentDto::getOrderNum, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(DepartmentDto::getName));
        for (DepartmentDto node : nodes) {
            sortTree(node.getChildren());
        }
    }

    private boolean isChildOf(Department potentialParent, Long potentialChildId) {
        Department current = departmentRepository.findById(potentialChildId).orElse(null);
        while (current != null) {
            if (current.getId().equals(potentialParent.getId())) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }
}