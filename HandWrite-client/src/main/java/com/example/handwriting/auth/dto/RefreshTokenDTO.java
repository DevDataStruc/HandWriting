package com.example.handwriting.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
public class RefreshTokenDTO implements Serializable {

    @NotBlank
    private String refreshToken;
}
