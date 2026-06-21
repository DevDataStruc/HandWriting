package com.example.handwriting.dashboard.controller;

import com.example.handwriting.common.result.R;
import com.example.handwriting.dashboard.dto.UpdateUserRolesDTO;
import com.example.handwriting.dashboard.service.AdminUserService;
import com.example.handwriting.dashboard.vo.AdminUserVO;
import com.example.handwriting.dashboard.vo.UserListResponse;
import com.example.handwriting.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员用户管理接口。
 * <p>对应前端 {@code UserManagementView} / {@code MemberRosterView}：</p>
 * <ul>
 *     <li>GET  /v1/admin/users              分页查询（支持关键字 + 状态过滤）</li>
 *     <li>PATCH /v1/admin/users/{id}/status 切换启用/禁用</li>
 *     <li>PUT  /v1/admin/users/{id}/roles   整体替换用户角色集合</li>
 * </ul>
 */
@Tag(name = "管理员-用户管理")
@RestController
@RequestMapping("/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "分页查询用户列表（关键字 / 状态）")
    @GetMapping
    public R<UserListResponse> list(@RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) Integer status,
                                    @RequestParam(defaultValue = "1") long pageNum,
                                    @RequestParam(defaultValue = "10") long pageSize) {
        return R.ok(adminUserService.listUsers(keyword, status, pageNum, pageSize));
    }

    @Operation(summary = "切换用户启用/禁用状态")
    @PatchMapping("/{id}/status")
    public R<AdminUserVO> toggleStatus(@AuthenticationPrincipal LoginUser loginUser,
                                        @PathVariable Long id) {
        return R.ok(adminUserService.toggleStatus(loginUser.getUserId(), id));
    }

    @Operation(summary = "更新用户角色（整体替换）",
               description = "传入的角色集合即为最终结果，未在列表中的角色关联会被删除。"
                       + "至少需要 1 个角色；操作者不能修改自己的角色。")
    @PutMapping("/{id}/roles")
    public R<AdminUserVO> updateRoles(@AuthenticationPrincipal LoginUser loginUser,
                                       @PathVariable Long id,
                                       @Valid @RequestBody UpdateUserRolesDTO dto) {
        return R.ok(adminUserService.updateUserRoles(loginUser.getUserId(), id, dto.getRoles()));
    }
}

