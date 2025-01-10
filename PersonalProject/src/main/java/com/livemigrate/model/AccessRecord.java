package com.livemigrate.model;

import lombok.Data;

import java.time.Instant;

@Data
public class AccessRecord {
    private Instant timestamp;
    private String accessType;  // "read", "update", "export"
    private String accessedBy;  // system or user identifier
    private String purpose;     // reason for access
}