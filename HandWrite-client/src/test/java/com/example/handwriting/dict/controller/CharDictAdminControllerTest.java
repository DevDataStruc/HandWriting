package com.example.handwriting.dict.controller;

import com.example.handwriting.common.exception.BizException;
import com.example.handwriting.common.result.ErrorCode;
import com.example.handwriting.common.result.R;
import com.example.handwriting.dict.dto.CharDictBatchCreateDTO;
import com.example.handwriting.dict.dto.CharDictImportResultVO;
import com.example.handwriting.dict.dto.CharDictUpsertDTO;
import com.example.handwriting.dict.entity.CharDict;
import com.example.handwriting.dict.service.CharDictFileParser;
import com.example.handwriting.dict.service.CharDictService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CharDictAdminController + CharDictFileController 接口契约测试
 * <p>
 * 直接调用 Controller 方法验证 R&lt;T&gt; 包装、参数路由与异常映射，
 * 不依赖 MockMvc / Spring 上下文，可作为接口冒烟测试的快速回归。
 * </p>
 */
class CharDictAdminControllerTest {

    private CharDictService charDictService;
    private CharDictFileParser charDictFileParser;
    private CharDictAdminController admin;
    private CharDictFileController file;

    @BeforeEach
    void setup() {
        charDictService = mock(CharDictService.class);
        charDictFileParser = mock(CharDictFileParser.class);
        admin = new CharDictAdminController(charDictService, charDictFileParser);
        file = new CharDictFileController(charDictFileParser);
    }

    /* ===================== 单条 ===================== */

    @Test
    @DisplayName("POST /v1/admin/dict/chars 单条新增 → R.ok 包装")
    void createOne() {
        CharDict saved = new CharDict();
        saved.setId(1L);
        saved.setCharValue("永");
        when(charDictService.create(any(CharDictUpsertDTO.class))).thenReturn(saved);

        CharDictUpsertDTO dto = new CharDictUpsertDTO();
        dto.setCharValue("永");
        dto.setCategory("HANZI");
        dto.setDifficulty(2);
        dto.setEnabled(1);

        R<CharDict> res = admin.create(dto);
        assertEquals(0, res.getCode());
        assertNotNull(res.getData());
        assertEquals("永", res.getData().getCharValue());
        verify(charDictService, times(1)).create(any(CharDictUpsertDTO.class));
    }

    @Test
    @DisplayName("POST /v1/admin/dict/chars/{id} 更新 → 返回更新后实体")
    void updateOne() {
        CharDict updated = new CharDict();
        updated.setId(5L);
        updated.setCharValue("爱");
        when(charDictService.update(any(Long.class), any(CharDictUpsertDTO.class))).thenReturn(updated);

        CharDictUpsertDTO dto = new CharDictUpsertDTO();
        dto.setCharValue("爱");
        dto.setCategory("HANZI");
        dto.setDifficulty(3);
        dto.setEnabled(1);

        R<CharDict> res = admin.update(5L, dto);
        assertEquals(0, res.getCode());
        assertEquals("爱", res.getData().getCharValue());
    }

    @Test
    @DisplayName("GET /v1/admin/dict/chars/{id} 详情 → 返回实体")
    void detail() {
        CharDict c = new CharDict();
        c.setId(7L);
        c.setCharValue("永");
        when(charDictService.detail(7L)).thenReturn(c);

        R<CharDict> res = admin.detail(7L);
        assertEquals(0, res.getCode());
        assertEquals(7L, res.getData().getId());
    }

    @Test
    @DisplayName("DELETE /v1/admin/dict/chars/{id} 删除 → R.ok")
    void deleteChar() {
        R<Void> res = admin.delete(99L);
        assertEquals(0, res.getCode());
        verify(charDictService).delete(99L);
    }

    /* ===================== 批量 ===================== */

    @Test
    @DisplayName("POST /v1/admin/dict/chars/batch 批量新增 → 透传 Service 返回值")
    void batchCreate() {
        CharDictImportResultVO vo = new CharDictImportResultVO();
        vo.setInserted(3);
        vo.setSkipped(1);
        vo.setFailed(0);
        when(charDictService.batchCreateWithDefaults(any(), any())).thenReturn(vo);

        CharDictBatchCreateDTO dto = new CharDictBatchCreateDTO();
        dto.setDefaultCategory("HANZI");
        List<CharDictBatchCreateDTO.Item> items = new ArrayList<>();
        items.add(item("A"));
        items.add(item("B"));
        items.add(item("C"));
        items.add(item("A"));
        dto.setItems(items);

        R<CharDictImportResultVO> res = admin.batchCreate(dto);
        assertEquals(0, res.getCode());
        assertEquals(3, res.getData().getInserted());
        assertEquals(1, res.getData().getSkipped());
        verify(charDictService, times(1)).batchCreateWithDefaults(any(), any());
    }

    /* ===================== JSON 文本 ===================== */

    @Test
    @DisplayName("POST /v1/admin/dict/chars/import/json → 解析 dto.items[0].charValue 作为 JSON 字符串")
    void importJson() {
        CharDictImportResultVO vo = new CharDictImportResultVO();
        vo.setInserted(2);
        vo.setMessage("ok");
        when(charDictFileParser.parseJsonStringAndImport(anyString(), any())).thenReturn(vo);

        CharDictBatchCreateDTO dto = new CharDictBatchCreateDTO();
        dto.setDefaultCategory("HANZI");
        List<CharDictBatchCreateDTO.Item> items = new ArrayList<>();
        items.add(item("[\"A\",\"B\"]"));
        dto.setItems(items);

        R<CharDictImportResultVO> res = admin.importJson(dto);
        assertEquals(0, res.getCode());
        assertEquals(2, res.getData().getInserted());
        verify(charDictFileParser).parseJsonStringAndImport(anyString(), any());
    }

    @Test
    @DisplayName("POST /v1/admin/dict/chars/import/json 空 DTO → 不会崩溃，返回 R.ok(空结果)")
    void importJsonEmpty() {
        CharDictImportResultVO vo = new CharDictImportResultVO();
        vo.setMessage("empty");
        when(charDictFileParser.parseJsonStringAndImport(anyString(), any())).thenReturn(vo);

        R<CharDictImportResultVO> res = admin.importJson(null);
        assertEquals(0, res.getCode());
    }

    /* ===================== 文件上传 ===================== */

    @Test
    @DisplayName("POST /v1/admin/dict/chars/import/file multipart → 调用 Parser")
    void importFile() {
        CharDictImportResultVO vo = new CharDictImportResultVO();
        vo.setInserted(4);
        when(charDictFileParser.parseAndImport(any(MultipartFile.class), any())).thenReturn(vo);

        MockMultipartFile f = new MockMultipartFile("file", "chars.txt", "text/plain",
                "永\n的\n我\n你\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        R<CharDictImportResultVO> res = file.importFile(f, "HANZI", 1, null, 1);
        assertEquals(0, res.getCode());
        assertEquals(4, res.getData().getInserted());
        verify(charDictFileParser).parseAndImport(any(MultipartFile.class), any());
    }

    @Test
    @DisplayName("POST /v1/admin/dict/chars/import/file 参数错误 → BizException(1000)")
    void importFileBadArg() {
        when(charDictFileParser.parseAndImport(any(MultipartFile.class), any()))
                .thenThrow(new IllegalArgumentException("文件大小超过限制（10MB）"));

        MockMultipartFile f = new MockMultipartFile("file", "big.txt", "text/plain", new byte[]{1, 2, 3});

        BizException ex = assertThrows(BizException.class,
                () -> file.importFile(f, null, null, null, null));
        assertEquals(ErrorCode.BAD_REQUEST.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("文件大小"));
    }

    @Test
    @DisplayName("POST /v1/admin/dict/chars/import/file 内部异常 → BizException(5000)")
    void importFileInternalError() {
        when(charDictFileParser.parseAndImport(any(MultipartFile.class), any()))
                .thenThrow(new RuntimeException("POI 解析失败"));

        MockMultipartFile f = new MockMultipartFile("file", "bad.xlsx", "application/octet-stream", new byte[]{0});

        BizException ex = assertThrows(BizException.class,
                () -> file.importFile(f, null, null, null, null));
        assertEquals(ErrorCode.INTERNAL_ERROR.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("POST /v1/admin/dict/chars/import/preview → 返回 Map 不抛异常")
    void preview() {
        java.util.Map<String, Object> fake = new java.util.HashMap<>();
        fake.put("total", 5);
        fake.put("ok", true);
        when(charDictFileParser.preview(any(MultipartFile.class), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(fake);

        MockMultipartFile f = new MockMultipartFile("file", "chars.txt", "text/plain",
                "a\nb\nc\nd\ne\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        R<java.util.Map<String, Object>> res = file.preview(f, 20);
        assertEquals(0, res.getCode());
        assertEquals(5, res.getData().get("total"));
    }

    @Test
    @DisplayName("POST /v1/admin/dict/chars/import/preview 解析失败 → 仍返回 R.ok({ok:false, message})")
    void previewWithException() {
        when(charDictFileParser.preview(any(MultipartFile.class), org.mockito.ArgumentMatchers.anyInt()))
                .thenThrow(new RuntimeException("boom"));

        MockMultipartFile f = new MockMultipartFile("file", "broken.txt", "text/plain", new byte[]{0});
        R<java.util.Map<String, Object>> res = file.preview(f, 10);
        assertEquals(0, res.getCode());
        assertEquals(false, res.getData().get("ok"));
        assertNotNull(res.getData().get("message"));
    }

    private CharDictBatchCreateDTO.Item item(String s) {
        CharDictBatchCreateDTO.Item it = new CharDictBatchCreateDTO.Item();
        it.setCharValue(s);
        it.setLineNo(1);
        return it;
    }
}
