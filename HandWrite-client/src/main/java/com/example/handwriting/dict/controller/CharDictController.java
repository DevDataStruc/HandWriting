package com.example.handwriting.dict.controller;

import com.example.handwriting.common.result.PageQuery;
import com.example.handwriting.common.result.PageResult;
import com.example.handwriting.common.result.R;
import com.example.handwriting.dict.entity.CharDict;
import com.example.handwriting.dict.service.CharDictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "字符字典")
@RestController
@RequestMapping("/v1/dict")
@RequiredArgsConstructor
public class CharDictController {

    private final CharDictService charDictService;

    @Operation(summary = "分页查询字符字典")
    @GetMapping("/chars")
    public R<PageResult<CharDict>> page(@RequestParam(required = false) String category, PageQuery query) {
        return R.ok(charDictService.page(category, query));
    }

    @Operation(summary = "按分类列出字符（缓存友好）")
    @GetMapping("/chars/list")
    public R<List<CharDict>> list(@RequestParam(required = false) String category) {
        return R.ok(charDictService.listByCategory(category));
    }
}
