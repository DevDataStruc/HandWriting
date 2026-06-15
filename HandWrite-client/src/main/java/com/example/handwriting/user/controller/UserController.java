package com.example.handwriting.user.controller;

import com.example.handwriting.common.result.R;
import com.example.handwriting.security.LoginUser;
import com.example.handwriting.user.dto.UserProfileDTO;
import com.example.handwriting.user.dto.UserVO;
import com.example.handwriting.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户")
@RestController
@RequestMapping("/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "获取个人信息")
    @GetMapping("/profile")
    public R<UserVO> profile(@AuthenticationPrincipal LoginUser loginUser) {
        return R.ok(userService.getProfile(loginUser.getUserId()));
    }

    @Operation(summary = "修改个人信息")
    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public R<UserVO> updateProfile(@AuthenticationPrincipal LoginUser loginUser,
                                   @RequestBody @Valid UserProfileDTO dto) {
        return R.ok(userService.updateProfile(loginUser.getUserId(), dto));
    }
}
