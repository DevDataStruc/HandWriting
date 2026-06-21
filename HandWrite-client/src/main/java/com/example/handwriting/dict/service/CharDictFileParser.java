package com.example.handwriting.dict.service;

import com.example.handwriting.dict.dto.CharDictBatchCreateDTO;
import com.example.handwriting.dict.dto.CharDictImportResultVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 字符字典文件解析器
 * <p>
 * 支持格式：
 * <ul>
 *   <li>纯文本（.txt / 任意文本）: 一行一字符；支持注释行（# 开头）与空行</li>
 *   <li>JSON: 数组，元素可为字符串或 {charValue, category, difficulty, description, enabled} 对象</li>
 *   <li>Excel（.xls / .xlsx）: 列头建议 charValue, category, difficulty, description, enabled；可不带列头按位置取</li>
 *   <li>Word（.docx）: 每个段落或换行符分隔的字符</li>
 * </ul>
 */
@Slf4j
@Service
public class CharDictFileParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final int MAX_SAMPLES = 50;
    private static final int MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private final CharDictService charDictService;

    public CharDictFileParser(CharDictService charDictService) {
        this.charDictService = charDictService;
    }

    /**
     * 解析并入库
     *
     * @param file  上传文件（txt/json/xls/xlsx/docx）
     * @param dto   批量 DTO（提供默认值）
     * @return 导入结果
     */
    public CharDictImportResultVO parseAndImport(MultipartFile file, CharDictBatchCreateDTO dto) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件大小超过限制（10MB）");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        List<CharDictBatchCreateDTO.Item> items;
        try {
            if (name.endsWith(".json")) {
                items = parseJson(file);
            } else if (name.endsWith(".xlsx") || name.endsWith(".xls")) {
                items = parseExcel(file);
            } else if (name.endsWith(".docx")) {
                items = parseDocx(file);
            } else if (name.endsWith(".doc")) {
                throw new IllegalArgumentException("暂不支持旧版 .doc 二进制格式，请另存为 .docx 后重试");
            } else {
                // 兜底按纯文本处理（兼容 .txt / 无扩展名 / 未知扩展名）
                items = parseText(file);
            }
        } catch (IOException e) {
            log.error("[CharDictFileParser] 解析失败", e);
            throw new RuntimeException("文件解析失败: " + e.getMessage(), e);
        }

        if (items == null || items.isEmpty()) {
            CharDictImportResultVO empty = new CharDictImportResultVO();
            empty.setMessage("未在文件中识别到任何字符");
            return empty;
        }
        return charDictService.batchCreateWithDefaults(dto, items);
    }

    /**
     * 解析 JSON 字符串并入库（无需上传文件，前端可直接传 JSON 文本）
     */
    public CharDictImportResultVO parseJsonStringAndImport(String jsonText, CharDictBatchCreateDTO dto) {
        if (jsonText == null || jsonText.isBlank()) {
            throw new IllegalArgumentException("JSON 内容为空");
        }
        List<CharDictBatchCreateDTO.Item> items;
        try {
            items = parseJsonText(jsonText);
        } catch (IOException e) {
            throw new IllegalArgumentException("JSON 解析失败: " + e.getMessage());
        }
        if (items.isEmpty()) {
            CharDictImportResultVO empty = new CharDictImportResultVO();
            empty.setMessage("JSON 中未识别到任何字符");
            return empty;
        }
        return charDictService.batchCreateWithDefaults(dto, items);
    }

    /* ===================== TXT ===================== */

    public List<CharDictBatchCreateDTO.Item> parseText(MultipartFile file) throws IOException {
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        return splitByCharacters(content);
    }

    private List<CharDictBatchCreateDTO.Item> splitByCharacters(String content) {
        if (content == null) return Collections.emptyList();
        // 同时支持 \n 与 \r\n 两种换行
        String[] lines = content.split("\\r?\\n");
        List<CharDictBatchCreateDTO.Item> items = new ArrayList<>();
        int lineNo = 0;
        for (String raw : lines) {
            lineNo++;
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            // 同一行可能包含多个字符（例如 "你好"），逐字符拆开便于录入
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                // 跳过空白与不可打印字符
                if (Character.isWhitespace(c)) continue;
                if (!isPrintable(c)) continue;
                CharDictBatchCreateDTO.Item item = new CharDictBatchCreateDTO.Item();
                item.setCharValue(String.valueOf(c));
                item.setLineNo(lineNo);
                items.add(item);
            }
        }
        return items;
    }

    private boolean isPrintable(char c) {
        return c >= 0x20 && c != 0x7F;
    }

    /* ===================== JSON ===================== */

    public List<CharDictBatchCreateDTO.Item> parseJson(MultipartFile file) throws IOException {
        return parseJsonText(new String(file.getBytes(), StandardCharsets.UTF_8));
    }

    public List<CharDictBatchCreateDTO.Item> parseJsonText(String jsonText) throws IOException {
        JsonNode root;
        try {
            root = MAPPER.readTree(jsonText);
        } catch (JsonProcessingException e) {
            throw new IOException("JSON 格式错误: " + e.getOriginalMessage(), e);
        }
        if (root == null || !root.isArray()) {
            throw new IOException("JSON 必须是数组（数组元素可为字符串或对象）");
        }
        List<CharDictBatchCreateDTO.Item> items = new ArrayList<>();
        int idx = 0;
        Iterator<JsonNode> it = root.elements();
        while (it.hasNext()) {
            idx++;
            JsonNode n = it.next();
            CharDictBatchCreateDTO.Item item = new CharDictBatchCreateDTO.Item();
            item.setLineNo(idx);
            if (n.isTextual()) {
                String val = n.asText();
                // 字符串中可能含多字符（"你好"），拆开
                for (int i = 0; i < val.length(); i++) {
                    char c = val.charAt(i);
                    if (Character.isWhitespace(c)) continue;
                    if (!isPrintable(c)) continue;
                    CharDictBatchCreateDTO.Item sub = new CharDictBatchCreateDTO.Item();
                    sub.setCharValue(String.valueOf(c));
                    sub.setLineNo(idx);
                    items.add(sub);
                }
            } else if (n.isObject()) {
                item.setCharValue(textOrNull(n, "charValue", "char", "value", "text"));
                item.setCategory(textOrNull(n, "category", "cat", "type"));
                item.setDifficulty(intOrNull(n, "difficulty", "level"));
                item.setDescription(textOrNull(n, "description", "desc", "remark"));
                item.setEnabled(intOrNull(n, "enabled", "active"));
                if (item.getCharValue() == null || item.getCharValue().isBlank()) {
                    throw new IOException("第 " + idx + " 项缺少 charValue 字段");
                }
                items.add(item);
            } else {
                throw new IOException("第 " + idx + " 项既不是字符串也不是对象");
            }
        }
        return items;
    }

    private String textOrNull(JsonNode n, String... names) {
        for (String name : names) {
            JsonNode v = n.get(name);
            if (v != null && !v.isNull()) return v.asText();
        }
        return null;
    }

    private Integer intOrNull(JsonNode n, String... names) {
        for (String name : names) {
            JsonNode v = n.get(name);
            if (v != null && v.isNumber()) return v.intValue();
        }
        return null;
    }

    /* ===================== EXCEL ===================== */

    public List<CharDictBatchCreateDTO.Item> parseExcel(MultipartFile file) throws IOException {
        try (InputStream in = file.getInputStream();
             Workbook wb = WorkbookFactory.create(in)) {
            FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) return Collections.emptyList();

            List<CharDictBatchCreateDTO.Item> items = new ArrayList<>();
            Iterator<Row> rowIt = sheet.rowIterator();
            // 先尝试识别表头
            Map<String, Integer> headerMap = null;
            int lineNo = 0;
            if (rowIt.hasNext()) {
                Row first = rowIt.next();
                lineNo++;
                headerMap = tryReadHeader(first, formatter);
            }
            while (rowIt.hasNext()) {
                Row row = rowIt.next();
                lineNo++;
                if (isRowEmpty(row, formatter)) continue;
                CharDictBatchCreateDTO.Item item = readRow(row, formatter, evaluator, headerMap, lineNo);
                if (item != null && item.getCharValue() != null && !item.getCharValue().isBlank()) {
                    items.add(item);
                }
            }
            return items;
        }
    }

    private Map<String, Integer> tryReadHeader(Row row, DataFormatter formatter) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell == null) continue;
            String text = formatter.formatCellValue(cell).trim().toLowerCase(Locale.ROOT);
            if (text.isEmpty()) continue;
            // 命中已知列名
            if (Arrays.asList("charvalue", "char", "value", "text", "字符", "字", "汉字").contains(text)) {
                map.put("charValue", i);
            } else if (Arrays.asList("category", "cat", "type", "分类").contains(text)) {
                map.put("category", i);
            } else if (Arrays.asList("difficulty", "level", "难度").contains(text)) {
                map.put("difficulty", i);
            } else if (Arrays.asList("description", "desc", "remark", "描述", "备注").contains(text)) {
                map.put("description", i);
            } else if (Arrays.asList("enabled", "active", "启用").contains(text)) {
                map.put("enabled", i);
            }
        }
        // 只有当首行包含 charValue 列时才视为表头，避免数据被误识别为表头
        return map.containsKey("charValue") ? map : null;
    }

    private CharDictBatchCreateDTO.Item readRow(Row row, DataFormatter formatter,
                                               FormulaEvaluator evaluator,
                                               Map<String, Integer> headerMap, int lineNo) {
        CharDictBatchCreateDTO.Item item = new CharDictBatchCreateDTO.Item();
        item.setLineNo(lineNo);

        String charValue;
        if (headerMap != null && headerMap.containsKey("charValue")) {
            charValue = readCell(row, headerMap.get("charValue"), formatter, evaluator);
        } else {
            // 无表头：第 0 列视为字符
            charValue = readCell(row, 0, formatter, evaluator);
        }
        if (charValue == null || charValue.isBlank()) return null;
        // 单行多字符（"你好"），逐字符拆开
        if (charValue.length() > 1) {
            CharDictBatchCreateDTO.Item first = null;
            for (int i = 0; i < charValue.length(); i++) {
                char c = charValue.charAt(i);
                if (Character.isWhitespace(c) || !isPrintable(c)) continue;
                CharDictBatchCreateDTO.Item sub = new CharDictBatchCreateDTO.Item();
                sub.setCharValue(String.valueOf(c));
                sub.setLineNo(lineNo);
                if (first == null) {
                    first = sub;
                } else {
                    first = null; // 标记为多字符
                }
            }
            if (first == null) {
                // 多字符：直接整段入库即可（DTO 允许长度 ≤ 8）
                item.setCharValue(charValue);
            } else {
                item.setCharValue(first.getCharValue());
            }
        } else {
            item.setCharValue(charValue);
        }
        if (headerMap != null) {
            Integer idx;
            if ((idx = headerMap.get("category")) != null) item.setCategory(readCell(row, idx, formatter, evaluator));
            if ((idx = headerMap.get("difficulty")) != null) item.setDifficulty(parseIntSafe(readCell(row, idx, formatter, evaluator)));
            if ((idx = headerMap.get("description")) != null) item.setDescription(readCell(row, idx, formatter, evaluator));
            if ((idx = headerMap.get("enabled")) != null) item.setEnabled(parseIntSafe(readCell(row, idx, formatter, evaluator)));
        }
        return item;
    }

    private boolean isRowEmpty(Row row, DataFormatter formatter) {
        if (row == null) return true;
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell == null) continue;
            String text = formatter.formatCellValue(cell);
            if (text != null && !text.trim().isEmpty()) return false;
        }
        return true;
    }

    private String readCell(Row row, int col, DataFormatter formatter, FormulaEvaluator evaluator) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.FORMULA && evaluator != null) {
            try {
                return formatter.formatCellValue(cell, evaluator);
            } catch (Exception ex) {
                return formatter.formatCellValue(cell);
            }
        }
        return formatter.formatCellValue(cell).trim();
    }

    private Integer parseIntSafe(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /* ===================== DOCX ===================== */

    public List<CharDictBatchCreateDTO.Item> parseDocx(MultipartFile file) throws IOException {
        try (InputStream in = file.getInputStream();
             XWPFDocument doc = new XWPFDocument(in)) {
            StringBuilder sb = new StringBuilder();
            for (XWPFParagraph p : doc.getParagraphs()) {
                sb.append(p.getText()).append('\n');
            }
            // 表格中的文本也一并抓取
            doc.getTables().forEach(t -> t.getRows().forEach(r -> r.getTableCells().forEach(c -> {
                sb.append(c.getText()).append('\n');
            })));
            return splitByCharacters(sb.toString());
        }
    }

    /* ===================== 公共 ===================== */

    public Map<String, Object> preview(MultipartFile file, int limit) {
        Map<String, Object> result = new HashMap<>();
        try {
            String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
            List<CharDictBatchCreateDTO.Item> items;
            if (name.endsWith(".json")) {
                items = parseJson(file);
            } else if (name.endsWith(".xlsx") || name.endsWith(".xls")) {
                items = parseExcel(file);
            } else if (name.endsWith(".docx")) {
                items = parseDocx(file);
            } else {
                items = parseText(file);
            }
            int n = Math.min(items.size(), Math.max(limit, 0));
            result.put("total", items.size());
            result.put("preview", items.subList(0, n));
            result.put("ok", true);
        } catch (Exception e) {
            result.put("ok", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
