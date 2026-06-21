package com.example.handwriting.dashboard.service;

import com.example.handwriting.admin.entity.Role;
import com.example.handwriting.admin.entity.UserRole;
import com.example.handwriting.admin.repository.RoleRepository;
import com.example.handwriting.admin.repository.UserRoleRepository;
import com.example.handwriting.common.constant.CommonConstants;
import com.example.handwriting.common.exception.BizException;
import com.example.handwriting.common.result.ErrorCode;
import com.example.handwriting.dashboard.vo.AdminUserVO;
import com.example.handwriting.dashboard.vo.UserListResponse;
import com.example.handwriting.user.entity.User;
import com.example.handwriting.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理员用户管理服务。
 * <p>对应前端 {@code GET /v1/admin/users} 等接口</p>
 */
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final com.example.handwriting.sample.repository.SampleRepository sampleRepository;

    /**
     * 分页查询用户列表
     *
     * @param keyword  关键字（匹配 username / nickname / email）
     * @param status   0 正常 / 1 禁用，null 表示全部
     * @param pageNum  页码（1-based）
     * @param pageSize 每页大小
     */
    @Transactional(readOnly = true)
    public UserListResponse listUsers(String keyword, Integer status, long pageNum, long pageSize) {
        if (pageNum <= 0) pageNum = 1L;
        if (pageSize <= 0) pageSize = 10L;
        if (pageSize > 100) pageSize = 100L;

        Pageable pageable = PageRequest.of((int) (pageNum - 1), (int) pageSize);
        Page<User> page = userRepository.searchAdmin(emptyToNull(keyword), status, pageable);

        List<User> users = page.getContent();
        if (users.isEmpty()) {
            return new UserListResponse(Collections.emptyList(), page.getTotalElements());
        }

        // 1) 批量查询用户-角色关联
        List<Long> userIds = users.stream().map(User::getId).collect(Collectors.toList());
        Map<Long, List<UserRole>> userRolesMap = userRoleRepository.findAll().stream()
                .filter(ur -> userIds.contains(ur.getUserId()))
                .collect(Collectors.groupingBy(UserRole::getUserId));

        // 2) 角色编码 -> 名称
        Map<Long, Role> roleMap = new HashMap<>();
        roleRepository.findAll().forEach(r -> roleMap.put(r.getId(), r));

        // 3) 样本数：按 user_id 聚合（简化版：按 ID 逐个 count，对小数据集足够）
        Map<Long, Long> sampleCountMap = new HashMap<>();
        for (Long uid : userIds) {
            long c = sampleRepository.findByUserId(uid,
                    PageRequest.of(0, 1)).getTotalElements();
            sampleCountMap.put(uid, c);
        }

        // 4) 组装 VO
        List<AdminUserVO> list = new ArrayList<>(users.size());
        for (User u : users) {
            AdminUserVO vo = new AdminUserVO();
            BeanUtils.copyProperties(u, vo);
            // 状态扩展
            vo.setStatusExt(u.getStatus() != null && u.getStatus() == 0 ? "active" : "disabled");
            // createdAt 镜像 createTime
            vo.setCreatedAt(u.getCreateTime() == null ? null : u.getCreateTime().toString());

            // 角色编码列表
            List<UserRole> urs = userRolesMap.getOrDefault(u.getId(), Collections.emptyList());
            List<String> roleCodes = urs.stream()
                    .map(ur -> roleMap.get(ur.getRoleId()))
                    .filter(r -> r != null)
                    .map(Role::getCode)
                    .collect(Collectors.toList());
            vo.setRoles(roleCodes);

            // 权限点：使用当前用户角色对应的权限（此处前端只显示角色名，可按需扩展）
            // 当前实现：仅返回角色编码作为 roles；permissions 暂留空，由前端根据 roles 自行解析
            vo.setPermissions(Collections.emptyList());

            vo.setSampleCount(sampleCountMap.getOrDefault(u.getId(), 0L));
            list.add(vo);
        }
        return new UserListResponse(list, page.getTotalElements());
    }

    /**
     * 切换用户启用/禁用状态
     *
     * @param operatorId 当前操作者（不能禁用自己）
     * @param userId     目标用户
     * @return 更新后的 VO
     */
    @Transactional
    public AdminUserVO toggleStatus(Long operatorId, Long userId) {
        if (operatorId != null && operatorId.equals(userId)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "不能修改自己的账号状态");
        }
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(ErrorCode.USER_NOT_EXISTS));
        u.setStatus(u.getStatus() != null && u.getStatus() == CommonConstants.USER_STATUS_NORMAL
                ? CommonConstants.USER_STATUS_DISABLED
                : CommonConstants.USER_STATUS_NORMAL);
        userRepository.save(u);
        return toVO(u);
    }

    private AdminUserVO toVO(User u) {
        AdminUserVO vo = new AdminUserVO();
        BeanUtils.copyProperties(u, vo);
        vo.setStatusExt(u.getStatus() != null && u.getStatus() == 0 ? "active" : "disabled");
        vo.setCreatedAt(u.getCreateTime() == null ? null : u.getCreateTime().toString());
        return vo;
    }

    private static String emptyToNull(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }
}
