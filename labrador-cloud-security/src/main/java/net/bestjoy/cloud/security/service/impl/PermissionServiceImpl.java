package net.bestjoy.cloud.security.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import net.bestjoy.cloud.core.error.BusinessException;
import net.bestjoy.cloud.core.generator.IDGenerator;
import net.bestjoy.cloud.security.context.SecurityContext;
import net.bestjoy.cloud.security.core.entitiy.*;
import net.bestjoy.cloud.security.core.enums.MenuStatusEnum;
import net.bestjoy.cloud.security.core.enums.MenuTypeEnum;
import net.bestjoy.cloud.security.core.enums.PermissionTypeEnum;
import net.bestjoy.cloud.security.persistent.repository.*;
import net.bestjoy.cloud.security.service.PermissionService;
import net.bestjoy.cloud.security.service.PermissionSubjectContext;
import net.bestjoy.cloud.security.service.PermissionSubjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

import static net.bestjoy.cloud.security.core.error.AuthErrors.OPERATION_NOT_FOUND_ERROR;


/**
 * @author ray
 */
@Slf4j
@Service
public class PermissionServiceImpl implements PermissionService {
    @Autowired
    private PermissionMapper permissionMapper;
    @Autowired
    private RolePermissionMapper rolePermissionMapper;
    @Autowired
    private OperationMapper operationMapper;
    @Autowired
    private PermissionOperationMapper permissionOperationMapper;
    @Autowired
    private MenuMapper menuMapper;
    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private UserRoleMapper userRoleMapper;
    @Autowired
    private PermissionSubjectContext permissionSubjectContext;

    @Override
    public void addRole(Role role) {
        role.setCreateTime(new Date());
        role.setUpdateTime(role.getCreateTime());
        role.setRoleId(IDGenerator.SNOW_FLAKE_STRING.generate());
        role.setSystemId(SecurityContext.getSystemId());
        roleMapper.insert(role);
    }

    @Override
    public void updateRole(Role role) {
        role.setUpdateTime(new Date());

        roleMapper.updateById(role);
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void deleteRole(String roleId) {
        QueryWrapper<Role> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("role_id", roleId);
        queryWrapper.eq("system_id", SecurityContext.getSystemId());
        roleMapper.delete(queryWrapper);

        QueryWrapper<UserRole> userRoleQueryWrapper = new QueryWrapper<>();
        userRoleQueryWrapper.eq("role_id", roleId);
        userRoleQueryWrapper.eq("system_id", SecurityContext.getSystemId());
        userRoleMapper.delete(userRoleQueryWrapper);
    }

    @Override
    public Role getRoleById(String roleId) {
        QueryWrapper<Role> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("role_id", roleId);
        queryWrapper.eq("system_id", SecurityContext.getSystemId());

        return roleMapper.selectOne(queryWrapper);
    }

    @Override
    public Role getRoleByName(String roleName) {
        QueryWrapper<Role> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("role_name", roleName);
        queryWrapper.eq("system_id", SecurityContext.getSystemId());

        return roleMapper.selectOne(queryWrapper);
    }

    @Override
    public List<Menu> getPublishedMenuList(String parentMenuId, MenuTypeEnum menuType) {
        QueryWrapper<Menu> queryWrapper = new QueryWrapper<>();
        if (menuType != null) {
            queryWrapper.eq("menu_type", menuType);
        }
        if (!StringUtils.isEmpty(parentMenuId)) {
            queryWrapper.eq("parent_menu_id", parentMenuId);
        }
        queryWrapper.eq("menu_status", MenuStatusEnum.PUBLISHED);
        queryWrapper.eq("system_id", SecurityContext.getSystemId());
        queryWrapper.orderByAsc("order_num").orderByDesc("create_time");

        return menuMapper.selectList(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void savePermission(Permission permission, PermissionTypeEnum permissionType, String subjectId, String operationId) {
        PermissionSubjectProvider permissionSubjectProvider =
                permissionSubjectContext.getPermissionSubjectProvider(permissionType);

        //判断权限主体是否存在
        permissionSubjectProvider.hasPermissionSubject(subjectId);
        //判断操作是否存在
        hasOperation(operationId);

        permission.setCreateTime(new Date());
        permission.setUpdateTime(permission.getCreateTime());
        permission.setPermissionId(IDGenerator.SNOW_FLAKE_STRING.generate());
        permission.setSystemId(SecurityContext.getSystemId());
        permissionMapper.insert(permission);

        //保存权限主体
        permissionSubjectProvider.addPermissionSubjectRel(subjectId, permission.getPermissionId());
        //保存权限操作
        addPermissionOperation(permission.getPermissionId(), operationId);
    }

    @Override
    public void updatePermission(Permission permission) {
        permission.setUpdateTime(new Date());

        permissionMapper.updateById(permission);
    }

    @Override
    public Permission getPermissionByName(String permissionName) {
        QueryWrapper<Permission> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("permission_name", permissionName);
        queryWrapper.eq("system_id", SecurityContext.getSystemId());

        return permissionMapper.selectOne(queryWrapper);
    }

    @Override
    public Permission getPermissionByPermissionId(String permissionId) {
        QueryWrapper<Permission> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("permission_id", permissionId);
        queryWrapper.eq("system_id", SecurityContext.getSystemId());

        return permissionMapper.selectOne(queryWrapper);
    }

    @Override
    public void addRolePermission(String roleId, String permissionId) {
        //判断是否已经存在
        QueryWrapper<RolePermission> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("role_id", roleId);
        queryWrapper.eq("permission_id", permissionId);
        queryWrapper.eq("system_id", SecurityContext.getSystemId());

        RolePermission rolePermission = rolePermissionMapper.selectOne(queryWrapper);
        if (rolePermission == null) {
            rolePermission = new RolePermission(roleId, permissionId);
            rolePermission.setCreateTime(new Date());
            rolePermission.setUpdateTime(rolePermission.getCreateTime());
            rolePermission.setSystemId(SecurityContext.getSystemId());
            log.info("角色 ‘{}’ 新增 权限 ‘{}’", roleId, permissionId);
            rolePermissionMapper.insert(rolePermission);
        }
    }

    @Override
    public void deleteRolePermission(String roleId, String permissionId) {
        QueryWrapper<RolePermission> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("role_id", roleId);
        queryWrapper.eq("permission_id", permissionId);
        queryWrapper.eq("system_id", SecurityContext.getSystemId());

        rolePermissionMapper.delete(queryWrapper);
    }

    @Override
    public IPage<Permission> queryRolePermission(Page<Permission> page, String roleId) {
        QueryWrapper<RolePermission> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("role_id", roleId);
        queryWrapper.eq("system_id", SecurityContext.getSystemId());
        queryWrapper.orderByDesc("create_time");

        return rolePermissionMapper.pageRolePermissionOfRole(page, queryWrapper);
    }

    @Override
    public boolean hasOperation(String operationId) {
        Operation operation = getOperationById(operationId);

        if (operation == null) {
            throw new BusinessException(OPERATION_NOT_FOUND_ERROR);
        }

        return true;
    }


    @Override
    public void saveOperation(Operation operation) {
        operation.setCreateTime(new Date());
        operation.setUpdateTime(operation.getCreateTime());
        operation.setOperationId(IDGenerator.SNOW_FLAKE_STRING.generate());
        operation.setSystemId(SecurityContext.getSystemId());
        operationMapper.insert(operation);
    }

    @Override
    public void updateOperation(Operation operation) {
        operation.setUpdateTime(new Date());

        operationMapper.updateById(operation);
    }

    @Override
    public void addPermissionOperation(String permissionId, String operationId) {

        QueryWrapper<PermissionOperation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("permission_id", permissionId);
        queryWrapper.eq("operation_id", operationId);
        queryWrapper.eq("system_id", SecurityContext.getSystemId());

        PermissionOperation permissionOperation = permissionOperationMapper.selectOne(queryWrapper);

        if (permissionOperation == null) {
            permissionOperation = new PermissionOperation(permissionId, operationId);
            permissionOperation.setCreateTime(new Date());
            permissionOperation.setUpdateTime(permissionOperation.getCreateTime());
            permissionOperation.setSystemId(SecurityContext.getSystemId());
            log.info("新增权限 ‘{}’ 的操作 ‘{}’", permissionId, operationId);
            permissionOperationMapper.insert(permissionOperation);
        }
    }

    @Override
    public void deletePermissionOperation(String permissionId, String operationId) {
        QueryWrapper<PermissionOperation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("permission_id", permissionId);
        queryWrapper.eq("operation_id", operationId);
        queryWrapper.eq("system_id", SecurityContext.getSystemId());

        permissionOperationMapper.delete(queryWrapper);
    }

    @Override
    public Operation getOperationById(String operationId) {
        QueryWrapper<Operation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("operation_id", operationId);
        queryWrapper.eq("system_id", SecurityContext.getSystemId());

        return operationMapper.selectOne(queryWrapper);
    }

    @Override
    public Operation getOperationByName(String operationName) {
        QueryWrapper<Operation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("operation_name", operationName);
        queryWrapper.eq("system_id", SecurityContext.getSystemId());

        return operationMapper.selectOne(queryWrapper);
    }
}
