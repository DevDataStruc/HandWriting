package com.example.handwriting.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class UserProfileDTO implements Serializable {

    @Size(max = 64)
    private String nickname;

    @Email
    @Size(max = 128)
    private String email;

    @Size(max = 20)
    private String phone;

    @Size(max = 255)
    private String avatar;
}
