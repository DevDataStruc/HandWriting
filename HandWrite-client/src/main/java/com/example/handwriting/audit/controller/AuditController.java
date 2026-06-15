package com.example.handwriting.audit.controller;

import com.example.handwriting.audit.dto.AuditDecisionDTO;
import com.example.handwriting.audit.service.AuditService;
import com.example.handwriting.common.result.PageQuery;
import com.example.handwriting.common.result.PageResult;
import com.example.handwriting.common.result.R;
import com.example.handwriting.sample.dto.SampleVO;
import com.example.handwriting.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "审核")
@RestController
@RequestMapping("/v1/audit")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('AUDITOR','ADMIN')")
public class AuditController {

    private final AuditService auditService;

    @Operation(summary = "待审核列表")
    @GetMapping("/pending")
    public R<PageResult<SampleVO>> pending(PageQuery query) {
        return R.ok(auditService.pending(query));
    }

    @Operation(summary = "审核历史")
    @GetMapping("/history")
    public R<PageResult<SampleVO>> history(@RequestParam(required = false) Integer status, PageQuery query) {
        return R.ok(auditService.history(status, query));
    }

    @Operation(summary = "审核通过")
    @PostMapping("/{id}/approve")
    public R<Void> approve(@AuthenticationPrincipal LoginUser loginUser, @PathVariable Long id) {
        auditService.approve(loginUser.getUserId(), id);
        return R.ok();
    }

    @Operation(summary = "审核驳回")
    @PostMapping("/{id}/reject")
    public R<Void> reject(@AuthenticationPrincipal LoginUser loginUser,
                          @PathVariable Long id,
                          @RequestBody(required = false) AuditDecisionDTO dto) {
        auditService.reject(loginUser.getUserId(), id, dto);
        return R.ok();
    }
}
