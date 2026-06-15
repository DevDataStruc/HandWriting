package com.example.handwriting.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "登录成功响应")
public class LoginVO implements Serializable {

    @Schema(description = "Access Token")
    private String accessToken;

    @Schema(description = "Refresh Token")
    private String refreshToken;

    @Schema(description = "Token 类型")
    private String tokenType = "Bearer";

    @Schema(description = "Access Token 过期秒数")
    private long expiresIn;

    @Schema(description = "用户信息")
    private UserInfo user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo implements Serializable {
        private Long id;
        private String username;
        private String nickname;
        private String avatar;
        private List<String> roles;
        private List<String> permissions;
    }
}
