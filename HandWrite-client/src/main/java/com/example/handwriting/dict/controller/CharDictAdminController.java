package com.example.handwriting.dict.controller;

import com.example.handwriting.common.result.R;
import com.example.handwriting.dict.dto.CharDictBatchCreateDTO;
import com.example.handwriting.dict.dto.CharDictImportResultVO;
import com.example.handwriting.dict.dto.CharDictUpsertDTO;
import com.example.handwriting.dict.entity.CharDict;
import com.example.handwriting.dict.service.CharDictFileParser;
import com.example.handwriting.dict.service.CharDictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "字符字典管理")
@RestController
@RequestMapping("/v1/admin/dict")
@RequiredArgsConstructor
public class CharDictAdminController {

    private final CharDictService charDictService;
    private final CharDictFileParser charDictFileParser;

    @Operation(summary = "查询单个字符详情")
    @GetMapping("/chars/{id}")
    public R<CharDict> detail(@PathVariable Long id) {
        return R.ok(charDictService.detail(id));
    }

    @Operation(summary = "新增字符")
    @PostMapping("/chars")
    public R<CharDict> create(@Valid @RequestBody CharDictUpsertDTO dto) {
        return R.ok(charDictService.create(dto));
    }

    @Operation(summary = "修改字符")
    @PutMapping("/chars/{id}")
    public R<CharDict> update(@PathVariable Long id, @Valid @RequestBody CharDictUpsertDTO dto) {
        return R.ok(charDictService.update(id, dto));
    }

    @Operation(summary = "删除字符")
    @DeleteMapping("/chars/{id}")
    public R<Void> delete(@PathVariable Long id) {
        charDictService.delete(id);
        return R.ok();
    }

    @Operation(summary = "批量新增字符（前端一次性提交多条）")
    @PostMapping("/chars/batch")
    public R<CharDictImportResultVO> batchCreate(@Valid @RequestBody CharDictBatchCreateDTO dto) {
        return R.ok(charDictService.batchCreateWithDefaults(dto, dto.getItems()));
    }

    @Operation(summary = "通过 JSON 文本导入字符（不依赖文件上传）")
    @PostMapping("/chars/import/json")
    public R<CharDictImportResultVO> importJson(@RequestBody CharDictBatchCreateDTO dto) {
        // 约定：dto.items[0].charValue 字段作为整段 JSON 文本传输，避免与 DTO 字段冲突
        String json = (dto != null && dto.getItems() != null && !dto.getItems().isEmpty())
                ? dto.getItems().get(0).getCharValue()
                : null;
        return R.ok(charDictFileParser.parseJsonStringAndImport(json, dto));
    }
}
