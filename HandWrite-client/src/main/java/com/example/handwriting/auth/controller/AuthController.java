package com.example.handwriting.auth.controller;

import com.example.handwriting.auth.dto.LoginDTO;
import com.example.handwriting.auth.dto.LoginVO;
import com.example.handwriting.auth.dto.RefreshTokenDTO;
import com.example.handwriting.auth.dto.RegisterDTO;
import com.example.handwriting.auth.service.AuthService;
import com.example.handwriting.auth.service.CaptchaService;
import com.example.handwriting.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "鉴权")
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;

    @Operation(summary = "获取图形验证码")
    @GetMapping("/captcha")
    public R<Map<String, Object>> captcha() {
        return R.ok(captchaService.generate());
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public R<LoginVO> login(@RequestBody @Valid LoginDTO dto) {
        return R.ok(authService.login(dto));
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public R<LoginVO> register(@RequestBody @Valid RegisterDTO dto) {
        return R.ok(authService.register(dto));
    }

    @Operation(summary = "刷新 Access Token")
    @PostMapping("/refresh")
    public R<LoginVO> refresh(@RequestBody @Valid RefreshTokenDTO dto) {
        return R.ok(authService.refresh(dto.getRefreshToken()));
    }

    @Operation(summary = "用户注销")
    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request,
                          @RequestHeader(value = "Authorization", required = false) String authHeader,
                          @RequestBody(required = false) RefreshTokenDTO body) {
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        authService.logout(token, body == null ? null : body.getRefreshToken());
        return R.ok();
    }
}
