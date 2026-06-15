package com.example.handwriting.audit.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class AuditDecisionDTO implements Serializable {

    @Size(max = 255)
    private String reason;
}
