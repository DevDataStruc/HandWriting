package com.example.handwriting.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 更新用户角色请求体（PUT /v1/admin/users/{id}/roles）
 * <p>前端 MemberRosterView 编辑身份弹窗提交</p>
 *
 * <p>角色编码必须来自 {@link com.example.handwriting.common.constant.CommonConstants}
 * 中定义的 {@code ROLE_USER / ROLE_AUDITOR / ROLE_ADMIN} 三种之一。</p>
 */
@Data
@Schema(description = "更新用户角色请求")
public class UpdateUserRolesDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "角色编码列表（至少 1 个，合法值：USER / AUDITOR / ADMIN）",
            example = "[\"ADMIN\", \"USER\"]")
    @NotEmpty(message = "至少分配一个角色")
    @Size(max = 8, message = "单次最多 8 个角色")
    private List<String> roles;
}
