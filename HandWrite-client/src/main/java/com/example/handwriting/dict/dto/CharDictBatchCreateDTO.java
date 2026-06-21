package com.example.handwriting.dict.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 批量新增字符字典请求
 * <p>
 * 适用场景：管理员一次性提交多条字符，由后端统一去重、分类、入库。
 * </p>
 */
@Data
public class CharDictBatchCreateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 默认分类（列表项未指定 category 时使用，可选） */
    @Size(max = 32, message = "分类长度不能超过 32")
    private String defaultCategory;

    /** 默认难度（列表项未指定 difficulty 时使用，可选，默认 1） */
    @Max(5)
    private Integer defaultDifficulty;

    /** 默认描述 */
    @Size(max = 255, message = "描述长度不能超过 255")
    private String defaultDescription;

    /** 默认启用状态（可选，默认 1） */
    @Max(1)
    private Integer defaultEnabled;

    /** 字符条目（至少 1 条） */
    @NotEmpty(message = "字符列表不能为空")
    @Size(max = 5000, message = "单次批量最多 5000 条")
    @Valid
    private List<Item> items;

    /**
     * 字符条目
     * <ul>
     *   <li>charValue 必填</li>
     *   <li>category / difficulty / description / enabled 可缺省，回退到本 DTO 顶端的默认字段</li>
     * </ul>
     */
    @Data
    public static class Item implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @jakarta.validation.constraints.NotBlank(message = "字符不能为空")
        @jakarta.validation.constraints.Size(max = 8, message = "字符长度不能超过 8")
        private String charValue;

        @Size(max = 32, message = "分类长度不能超过 32")
        private String category;

        @Max(5)
        private Integer difficulty;

        @Size(max = 255, message = "描述长度不能超过 255")
        private String description;

        @Max(1)
        private Integer enabled;

        @NotNull
        private Integer lineNo;
    }
}
