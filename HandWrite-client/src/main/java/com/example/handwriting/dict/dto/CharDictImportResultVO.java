package com.example.handwriting.dict.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 字符字典导入结果 VO
 */
@Data
public class CharDictImportResultVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 实际新增成功数量 */
    private int inserted;

    /** 重复跳过数量 */
    private int skipped;

    /** 失败行数 */
    private int failed;

    /** 详细消息（便于前端展示） */
    private String message;

    /** 失败的字符样本（最多返回前 50 条，避免响应体过大） */
    private java.util.List<String> failedSamples;
}
