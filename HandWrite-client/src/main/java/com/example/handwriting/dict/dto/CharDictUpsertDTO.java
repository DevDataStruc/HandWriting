package com.example.handwriting.dict.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class CharDictUpsertDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank
    @Size(max = 8, message = "字符长度不能超过 8")
    private String charValue;

    @NotBlank
    @Size(max = 32, message = "分类长度不能超过 32")
    private String category;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer difficulty;

    @Size(max = 255, message = "描述长度不能超过 255")
    private String description;

    @NotNull
    @Min(0)
    @Max(1)
    private Integer enabled;
}
