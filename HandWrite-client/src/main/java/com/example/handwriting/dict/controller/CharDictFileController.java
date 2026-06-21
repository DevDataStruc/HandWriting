package com.example.handwriting.dict.controller;

import com.example.handwriting.common.exception.BizException;
import com.example.handwriting.common.result.ErrorCode;
import com.example.handwriting.common.result.R;
import com.example.handwriting.dict.dto.CharDictBatchCreateDTO;
import com.example.handwriting.dict.dto.CharDictImportResultVO;
import com.example.handwriting.dict.service.CharDictFileParser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 字符字典文件导入
 * <p>
 * 与 CharDictAdminController 拆分原因：文件上传走 multipart/form-data，
 * 与 JSON 接口分离后可让 Content-Type 协商更清晰，也便于安全/审计策略分别配置。
 * </p>
 */
@Tag(name = "字符字典管理")
@RestController
@RequestMapping("/v1/admin/dict")
@RequiredArgsConstructor
public class CharDictFileController {

    private final CharDictFileParser parser;

    @Operation(summary = "通过文件导入字符（支持 txt / xls / xlsx / docx / json）")
    @PostMapping(value = "/chars/import/file", consumes = "multipart/form-data")
    public R<CharDictImportResultVO> importFile(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "defaultCategory", required = false) String defaultCategory,
            @RequestParam(value = "defaultDifficulty", required = false) Integer defaultDifficulty,
            @RequestParam(value = "defaultDescription", required = false) String defaultDescription,
            @RequestParam(value = "defaultEnabled", required = false) Integer defaultEnabled) {
        try {
            CharDictBatchCreateDTO dto = new CharDictBatchCreateDTO();
            dto.setDefaultCategory(defaultCategory);
            dto.setDefaultDifficulty(defaultDifficulty);
            dto.setDefaultDescription(defaultDescription);
            dto.setDefaultEnabled(defaultEnabled);
            return R.ok(parser.parseAndImport(file, dto));
        } catch (IllegalArgumentException ex) {
            throw new BizException(ErrorCode.BAD_REQUEST, ex.getMessage());
        } catch (RuntimeException ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, ex.getMessage());
        }
    }

    @Operation(summary = "预览文件解析结果（不入库，仅返回前 N 条）")
    @PostMapping(value = "/chars/import/preview", consumes = "multipart/form-data")
    public R<java.util.Map<String, Object>> preview(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "limit", required = false, defaultValue = "20") int limit) {
        try {
            return R.ok(parser.preview(file, limit));
        } catch (Exception ex) {
            java.util.Map<String, Object> err = new java.util.HashMap<>();
            err.put("ok", false);
            err.put("message", ex.getMessage());
            return R.ok(err);
        }
    }
}
