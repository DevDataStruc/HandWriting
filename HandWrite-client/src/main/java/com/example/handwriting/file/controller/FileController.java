package com.example.handwriting.file.controller;

import com.example.handwriting.common.result.R;
import com.example.handwriting.file.dto.FileSignDTO;
import com.example.handwriting.file.dto.FileSignVO;
import com.example.handwriting.file.service.FileSignService;
import com.example.handwriting.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "文件")
@RestController
@RequestMapping("/v1/file")
@RequiredArgsConstructor
public class FileController {

    private final FileSignService fileSignService;

    @Operation(summary = "获取对象存储直传签名")
    @PostMapping("/sign")
    @PreAuthorize("isAuthenticated()")
    public R<FileSignVO> sign(@AuthenticationPrincipal LoginUser loginUser,
                               @RequestBody @Valid FileSignDTO dto) {
        return R.ok(fileSignService.sign(loginUser.getUserId(), dto));
    }
}
