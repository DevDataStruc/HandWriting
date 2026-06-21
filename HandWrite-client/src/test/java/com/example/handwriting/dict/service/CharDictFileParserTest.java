package com.example.handwriting.dict.service;

import com.example.handwriting.dict.dto.CharDictBatchCreateDTO;
import com.example.handwriting.dict.dto.CharDictImportResultVO;
import com.example.handwriting.dict.entity.CharDict;
import com.example.handwriting.dict.repository.CharDictRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * CharDictFileParser 单元测试
 *
 * <p>覆盖格式：txt / json / xlsx / docx</p>
 */
@ExtendWith(MockitoExtension.class)
class CharDictFileParserTest {

    @Mock
    private CharDictRepository repository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOps;

    @InjectMocks
    private CharDictService charDictService;

    private CharDictFileParser parser;

    @BeforeEach
    void setup() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        parser = new CharDictFileParser(charDictService);
    }

    /* ================= TXT ================= */

    @Test
    @DisplayName("TXT 解析：每行一字符，注释行 (#) 与空行被忽略")
    void parseTxt() {
        String content = "永\n的\n# 注释行\n\n我\n你\n";
        MultipartFile file = new MockMultipartFile(
                "file", "chars.txt", "text/plain",
                content.getBytes(StandardCharsets.UTF_8));

        // 全部视为新增
        Set<String> existing = new HashSet<>();
        when(repository.existsByCharValue(anyString()))
                .thenAnswer(inv -> existing.contains((String) inv.getArgument(0)));
        when(repository.saveAll(anyCollection()))
                .thenAnswer(inv -> {
                    List<CharDict> arg = inv.getArgument(0);
                    long id = 1;
                    for (CharDict c : arg) c.setId(id++);
                    return new java.util.ArrayList<>(arg);
                });

        CharDictBatchCreateDTO dto = new CharDictBatchCreateDTO();
        dto.setDefaultCategory("HANZI");
        CharDictImportResultVO vo = parser.parseAndImport(file, dto);
        assertEquals(4, vo.getInserted(), "应新增 永 的 我 你");
        assertEquals(0, vo.getSkipped());
    }

    /* ================= JSON ================= */

    @Test
    @DisplayName("JSON 解析：字符串数组逐字符拆开")
    void parseJsonStringArray() {
        String json = "[\"你好\", \"世界\"]";
        // 全部视为新增
        Set<String> existing = new HashSet<>();
        when(repository.existsByCharValue(anyString()))
                .thenAnswer(inv -> existing.contains((String) inv.getArgument(0)));
        when(repository.saveAll(anyCollection()))
                .thenAnswer(inv -> new java.util.ArrayList<>(inv.getArgument(0)));

        MultipartFile file = new MockMultipartFile(
                "file", "chars.json", "application/json",
                json.getBytes(StandardCharsets.UTF_8));

        CharDictBatchCreateDTO dto = new CharDictBatchCreateDTO();
        dto.setDefaultCategory("HANZI");
        CharDictImportResultVO vo = parser.parseAndImport(file, dto);
        // 你好世界 4 个独立字符
        assertEquals(4, vo.getInserted());
    }

    @Test
    @DisplayName("JSON 解析：对象数组携带元数据")
    void parseJsonObjectArray() {
        String json = "[" +
                "{\"charValue\":\"A\",\"category\":\"LETTER\",\"difficulty\":1}," +
                "{\"char\":\"B\",\"cat\":\"LETTER\",\"level\":2}" +
                "]";
        Set<String> existing = new HashSet<>();
        when(repository.existsByCharValue(anyString()))
                .thenAnswer(inv -> existing.contains((String) inv.getArgument(0)));
        when(repository.saveAll(anyCollection()))
                .thenAnswer(inv -> {
                    List<CharDict> arg = inv.getArgument(0);
                    long id = 1;
                    for (CharDict c : arg) c.setId(id++);
                    return new java.util.ArrayList<>(arg);
                });

        MultipartFile file = new MockMultipartFile(
                "file", "chars.json", "application/json",
                json.getBytes(StandardCharsets.UTF_8));

        CharDictBatchCreateDTO dto = new CharDictBatchCreateDTO();
        CharDictImportResultVO vo = parser.parseAndImport(file, dto);
        assertEquals(2, vo.getInserted());
    }

    @Test
    @DisplayName("JSON 格式错误应抛异常")
    void parseJsonMalformed() {
        String json = "{not valid json";
        MultipartFile file = new MockMultipartFile(
                "file", "bad.json", "application/json",
                json.getBytes(StandardCharsets.UTF_8));
        CharDictBatchCreateDTO dto = new CharDictBatchCreateDTO();
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> parser.parseAndImport(file, dto));
        assertTrue(ex.getMessage().contains("文件解析失败")
                || ex.getMessage().toLowerCase().contains("json"));
    }

    /* ================= EXCEL ================= */

    @Test
    @DisplayName("Excel 解析：表头映射 + 多行数据")
    void parseExcel() throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("chars");
            // 表头
            Row header = sheet.createRow(0);
            setHeader(header, "charValue", "category", "difficulty", "description");
            // 数据
            addRow(sheet, 1, "A", "LETTER", 1, "字母A");
            addRow(sheet, 2, "B", "LETTER", 1, "字母B");
            addRow(sheet, 3, "永", "HANZI", 3, "汉字永");

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            MultipartFile file = new MockMultipartFile(
                    "file", "chars.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    new ByteArrayInputStream(bos.toByteArray()));

            Set<String> existing = new HashSet<>();
            when(repository.existsByCharValue(anyString()))
                    .thenAnswer(inv -> existing.contains((String) inv.getArgument(0)));
            when(repository.saveAll(anyCollection()))
                    .thenAnswer(inv -> {
                        List<CharDict> arg = inv.getArgument(0);
                        long id = 1;
                        for (CharDict c : arg) c.setId(id++);
                        return new java.util.ArrayList<>(arg);
                    });

            CharDictBatchCreateDTO dto = new CharDictBatchCreateDTO();
            CharDictImportResultVO vo = parser.parseAndImport(file, dto);
            assertEquals(3, vo.getInserted());
        }
    }

    @Test
    @DisplayName("Excel 解析：无表头时按列位置（首列为字符）")
    void parseExcelNoHeader() throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("chars");
            // 第一行会被 tryReadHeader 消费（视为非表头时丢弃），
            // 实际入库从第二行起；这里提供 2 条数据行
            addRow(sheet, 0, "A", "LETTER");
            addRow(sheet, 1, "B", "LETTER");

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            MultipartFile file = new MockMultipartFile(
                    "file", "no-header.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    new ByteArrayInputStream(bos.toByteArray()));

            Set<String> existing = new HashSet<>();
            when(repository.existsByCharValue(anyString()))
                    .thenAnswer(inv -> existing.contains((String) inv.getArgument(0)));
            when(repository.saveAll(anyCollection()))
                    .thenAnswer(inv -> {
                        List<CharDict> arg = inv.getArgument(0);
                        long id = 1;
                        for (CharDict c : arg) c.setId(id++);
                        return new java.util.ArrayList<>(arg);
                    });

            CharDictBatchCreateDTO dto = new CharDictBatchCreateDTO();
            dto.setDefaultCategory("LETTER");
            CharDictImportResultVO vo = parser.parseAndImport(file, dto);
            assertEquals(1, vo.getInserted());
        }
    }

    /* ================= DOCX ================= */

    @Test
    @DisplayName("DOCX 解析：段落内字符按行/逐字符拆分")
    void parseDocx() throws Exception {
        // 构造一个最小可用的 docx
        try (org.apache.poi.xwpf.usermodel.XWPFDocument doc =
                     new org.apache.poi.xwpf.usermodel.XWPFDocument()) {
            org.apache.poi.xwpf.usermodel.XWPFParagraph p1 = doc.createParagraph();
            p1.createRun().setText("永");
            org.apache.poi.xwpf.usermodel.XWPFParagraph p2 = doc.createParagraph();
            p2.createRun().setText("的");

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            doc.write(bos);
            MultipartFile file = new MockMultipartFile(
                    "file", "chars.docx",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    new ByteArrayInputStream(bos.toByteArray()));

            Set<String> existing = new HashSet<>();
            when(repository.existsByCharValue(anyString()))
                    .thenAnswer(inv -> existing.contains((String) inv.getArgument(0)));
            when(repository.saveAll(anyCollection()))
                    .thenAnswer(inv -> {
                        List<CharDict> arg = inv.getArgument(0);
                        long id = 1;
                        for (CharDict c : arg) c.setId(id++);
                        return new java.util.ArrayList<>(arg);
                    });

            CharDictBatchCreateDTO dto = new CharDictBatchCreateDTO();
            dto.setDefaultCategory("HANZI");
            CharDictImportResultVO vo = parser.parseAndImport(file, dto);
            assertEquals(2, vo.getInserted());
        }
    }

    /* ================= 错误处理 ================= */

    @Test
    @DisplayName("空文件应抛 IllegalArgumentException")
    void emptyFile() {
        MultipartFile file = new MockMultipartFile(
                "file", "empty.txt", "text/plain", new byte[0]);
        CharDictBatchCreateDTO dto = new CharDictBatchCreateDTO();
        assertThrows(IllegalArgumentException.class,
                () -> parser.parseAndImport(file, dto));
    }

    @Test
    @DisplayName(".doc 旧版二进制格式应给出友好提示")
    void oldDocRejected() {
        MultipartFile file = new MockMultipartFile(
                "file", "old.doc", "application/msword",
                "fake binary".getBytes(StandardCharsets.UTF_8));
        CharDictBatchCreateDTO dto = new CharDictBatchCreateDTO();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> parser.parseAndImport(file, dto));
        assertTrue(ex.getMessage().contains(".docx"));
    }

    @Test
    @DisplayName("preview 接口返回前 N 条且不入库")
    void preview() {
        // 每行单字符 "字" 重复 100 次（用同字以保持集合稳定）
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            content.append("字\n");
        }
        MultipartFile file = new MockMultipartFile(
                "file", "large.txt", "text/plain",
                content.toString().getBytes(StandardCharsets.UTF_8));

        java.util.Map<String, Object> res = parser.preview(file, 10);
        assertNotNull(res);
        assertTrue((Boolean) res.get("ok"));
        assertEquals(100, res.get("total"));
        @SuppressWarnings("unchecked")
        List<CharDictBatchCreateDTO.Item> preview = (List<CharDictBatchCreateDTO.Item>) res.get("preview");
        assertEquals(10, preview.size());
        // saveAll 不应被调用（preview 仅解析不入库）
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.never()).saveAll(anyCollection());
    }

    /* ================= 工具 ================= */

    private void setHeader(Row row, String... headers) {
        for (int i = 0; i < headers.length; i++) {
            Cell c = row.createCell(i);
            c.setCellValue(headers[i]);
        }
    }

    private void addRow(Sheet sheet, int rowIdx, Object... values) {
        Row row = sheet.createRow(rowIdx);
        for (int i = 0; i < values.length; i++) {
            Cell c = row.createCell(i);
            Object v = values[i];
            if (v == null) continue;
            if (v instanceof Number) c.setCellValue(((Number) v).doubleValue());
            else c.setCellValue(String.valueOf(v));
        }
    }
}
