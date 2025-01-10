package com.livemigrate.model;

import lombok.Data;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

// Original record structure (X)
@Data
public class CustomerRecordV1 {
    private UUID id;
    private Instant createdAt;
    private CustomerData customerData;
    private int checksum;
}

