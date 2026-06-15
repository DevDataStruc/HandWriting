package com.example.handwriting.sample.controller;

import com.example.handwriting.common.result.PageQuery;
import com.example.handwriting.common.result.PageResult;
import com.example.handwriting.common.result.R;
import com.example.handwriting.sample.dto.SampleUploadDTO;
import com.example.handwriting.sample.dto.SampleVO;
import com.example.handwriting.sample.service.SampleService;
import com.example.handwriting.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "手写体样本")
@RestController
@RequestMapping("/v1/sample")
@RequiredArgsConstructor
public class SampleController {

    private final SampleService sampleService;

    @Operation(summary = "上传样本（提交元数据，文件已通过直传上传至对象存储）")
    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('sample:upload') or hasRole('USER') or hasRole('ADMIN')")
    public R<SampleVO> upload(@AuthenticationPrincipal LoginUser loginUser,
                              @RequestBody @Valid SampleUploadDTO dto) {
        return R.ok(sampleService.upload(loginUser.getUserId(), dto));
    }

    @Operation(summary = "分页查询我的样本")
    @GetMapping("/page")
    public R<PageResult<SampleVO>> myPage(@AuthenticationPrincipal LoginUser loginUser,
                                          @RequestParam(required = false) Integer status,
                                          PageQuery query) {
        return R.ok(sampleService.mySamples(loginUser.getUserId(), status, query));
    }

    @Operation(summary = "样本详情")
    @GetMapping("/{id}")
    public R<SampleVO> detail(@AuthenticationPrincipal LoginUser loginUser, @PathVariable Long id) {
        boolean isAdminOrAuditor = loginUser.getRoles() != null
                && (loginUser.getRoles().contains("ADMIN") || loginUser.getRoles().contains("AUDITOR"));
        return R.ok(sampleService.detail(id, loginUser.getUserId(), isAdminOrAuditor));
    }

    @Operation(summary = "删除样本（仅本人 / 未审核通过）")
    @DeleteMapping("/{id}")
    public R<Void> delete(@AuthenticationPrincipal LoginUser loginUser, @PathVariable Long id) {
        boolean isAdmin = loginUser.getRoles() != null && loginUser.getRoles().contains("ADMIN");
        sampleService.delete(id, loginUser.getUserId(), isAdmin);
        return R.ok();
    }
}
