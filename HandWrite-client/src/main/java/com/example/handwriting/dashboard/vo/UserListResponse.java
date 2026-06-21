package com.example.handwriting.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 用户分页响应（GET /v1/admin/users）
 * <p>字段扁平为 { list, total } 以匹配前端 UserManagementView 的取数方式</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理员用户分页响应")
public class UserListResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户列表")
    private List<AdminUserVO> list;

    @Schema(description = "总记录数")
    private long total;
}
