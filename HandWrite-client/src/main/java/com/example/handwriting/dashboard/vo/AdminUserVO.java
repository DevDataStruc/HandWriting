package com.example.handwriting.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理员视图中的用户档案（GET /v1/admin/users）
 * <p>字段与前端 {@code UserProfile} 对齐</p>
 */
@Data
@Schema(description = "管理员用户档案")
public class AdminUserVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户 ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "头像 URL")
    private String avatar;

    /** 0 正常 / 1 禁用 */
    @Schema(description = "原始状态码（0 正常 / 1 禁用）")
    private Integer status;

    @Schema(description = "扩展状态：active / disabled")
    private String statusExt;

    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 前端别名，等价于 createTime */
    @Schema(description = "创建时间（别名）")
    private String createdAt;

    @Schema(description = "角色编码列表")
    private List<String> roles;

    @Schema(description = "权限点列表")
    private List<String> permissions;

    @Schema(description = "该用户提交的样本数")
    private Long sampleCount;
}
