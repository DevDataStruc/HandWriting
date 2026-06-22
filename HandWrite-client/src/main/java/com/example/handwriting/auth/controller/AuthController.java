package com.example.handwriting.auth.controller;

import com.example.handwriting.auth.dto.LoginDTO;
import com.example.handwriting.auth.dto.LoginVO;
import com.example.handwriting.auth.dto.RefreshTokenDTO;
import com.example.handwriting.auth.dto.RegisterDTO;
import com.example.handwriting.auth.service.AuthService;
import com.example.handwriting.auth.service.CaptchaService;
import com.example.handwriting.common.result.R;
import com.example.handwriting.recovery.dto.BindTotpDTO;
import com.example.handwriting.recovery.dto.ForgotPasswordStartDTO;
import com.example.handwriting.recovery.dto.ForgotPasswordStartVO;
import com.example.handwriting.recovery.dto.PasswordResetTicketVO;
import com.example.handwriting.recovery.dto.ResetPasswordDTO;
import com.example.handwriting.recovery.dto.SecurityQuestionVO;
import com.example.handwriting.recovery.dto.SetupSecurityQuestionsDTO;
import com.example.handwriting.recovery.dto.TotpSetupVO;
import com.example.handwriting.recovery.dto.TotpStatusVO;
import com.example.handwriting.recovery.dto.VerifyRecoveryCodeDTO;
import com.example.handwriting.recovery.dto.VerifySecurityQuestionsDTO;
import com.example.handwriting.recovery.dto.VerifyTotpDTO;
import com.example.handwriting.recovery.dto.VerifyTotpForResetDTO;
import com.example.handwriting.recovery.service.PasswordRecoveryService;
import com.example.handwriting.recovery.service.SecurityQuestionService;
import com.example.handwriting.recovery.service.TotpService;
import com.example.handwriting.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "鉴权")
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;
    private final PasswordRecoveryService passwordRecoveryService;
    private final SecurityQuestionService securityQuestionService;
    private final TotpService totpService;

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

    /* =============================================================
     *  密码找回（公开）
     * ============================================================= */

    @Operation(summary = "找回密码 - 发起（公开）")
    @PostMapping("/recovery/start")
    public R<ForgotPasswordStartVO> recoveryStart(@RequestBody @Valid ForgotPasswordStartDTO dto) {
        return R.ok(passwordRecoveryService.start(dto.getUsername()));
    }

    @Operation(summary = "找回密码 - 获取密保问题正文（公开，挑战 ID 换取，不暴露账号是否存在）")
    @GetMapping("/recovery/security-questions")
    public R<List<SecurityQuestionVO>> recoverySecurityQuestions(
            @RequestParam("challengeId") String challengeId) {
        return R.ok(passwordRecoveryService.getSecurityQuestions(challengeId));
    }

    @Operation(summary = "找回密码 - 校验密保问题（公开）")
    @PostMapping("/recovery/verify-security")
    public R<PasswordResetTicketVO> recoveryVerifySecurity(
            @RequestBody @Valid VerifySecurityQuestionsDTO dto) {
        return R.ok(passwordRecoveryService.verifySecurityQuestions(dto));
    }

    @Operation(summary = "找回密码 - 校验动态口令 / 恢复码（公开）")
    @PostMapping("/recovery/verify-totp")
    public R<PasswordResetTicketVO> recoveryVerifyTotp(
            @RequestBody @Valid VerifyTotpForResetDTO dto) {
        return R.ok(passwordRecoveryService.verifyTotp(dto));
    }

    @Operation(summary = "找回密码 - 重置密码（公开）")
    @PostMapping("/recovery/reset")
    public R<Void> recoveryReset(@RequestBody @Valid ResetPasswordDTO dto) {
        passwordRecoveryService.resetPassword(dto.getResetToken(), dto.getNewPassword());
        return R.ok();
    }

    /* =============================================================
     *  2FA 绑定（已登录）
     * ============================================================= */

    @Operation(summary = "我的 2FA 状态")
    @GetMapping("/totp/status")
    public R<TotpStatusVO> totpStatus(@AuthenticationPrincipal LoginUser user) {
        return R.ok(totpService.status(user.getUserId()));
    }

    @Operation(summary = "初始化 2FA 绑定（生成 secret / 恢复码 / 二维码）")
    @PostMapping("/totp/setup")
    public R<TotpSetupVO> totpSetup(@AuthenticationPrincipal LoginUser user) {
        return R.ok(totpService.setup(user.getUserId()));
    }

    @Operation(summary = "确认 2FA 绑定（输入动态口令完成绑定）")
    @PostMapping("/totp/bind")
    public R<Void> totpBind(@AuthenticationPrincipal LoginUser user,
                            @RequestBody @Valid BindTotpDTO dto) {
        totpService.confirmBinding(user.getUserId(), dto.getCode());
        return R.ok();
    }

    @Operation(summary = "解绑 2FA")
    @DeleteMapping("/totp/bind")
    public R<Void> totpUnbind(@AuthenticationPrincipal LoginUser user) {
        totpService.unbind(user.getUserId());
        return R.ok();
    }

    @Operation(summary = "使用动态口令二次校验（已登录态）")
    @PostMapping("/totp/verify")
    public R<Void> totpVerify(@AuthenticationPrincipal LoginUser user,
                              @RequestBody @Valid VerifyTotpDTO dto) {
        totpService.verifyCode(user.getUserId(), dto.getCode());
        return R.ok();
    }

    @Operation(summary = "使用一次性恢复码（已登录态）")
    @PostMapping("/totp/verify-recovery")
    public R<Void> totpVerifyRecovery(@AuthenticationPrincipal LoginUser user,
                                      @RequestBody @Valid VerifyRecoveryCodeDTO dto) {
        totpService.verifyAndConsumeRecoveryCode(user.getUserId(), dto.getRecoveryCode());
        return R.ok();
    }

    /* =============================================================
     *  密保问题（已登录）
     * ============================================================= */

    @Operation(summary = "我的密保问题列表")
    @GetMapping("/security-questions")
    public R<List<SecurityQuestionVO>> listSq(@AuthenticationPrincipal LoginUser user) {
        return R.ok(securityQuestionService.listByUser(user.getUserId()));
    }

    @Operation(summary = "设置/覆盖密保问题（3 题）")
    @PostMapping("/security-questions")
    public R<Void> setupSq(@AuthenticationPrincipal LoginUser user,
                            @RequestBody @Valid SetupSecurityQuestionsDTO dto) {
        securityQuestionService.setup(user.getUserId(), dto);
        return R.ok();
    }

    @Operation(summary = "清空密保问题")
    @DeleteMapping("/security-questions")
    public R<Void> clearSq(@AuthenticationPrincipal LoginUser user) {
        securityQuestionService.clear(user.getUserId());
        return R.ok();
    }
}

