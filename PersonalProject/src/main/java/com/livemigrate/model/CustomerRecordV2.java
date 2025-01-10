package com.livemigrate.model;

import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class CustomerRecordV2 {
    private UUID id;
    private Instant createdAt;
    private CustomerData customerData;
    private int checksum;
    private short version;
    private Instant lastModified;
    // Enhanced metadata structure
    private RecordMetadata metadata;
}

