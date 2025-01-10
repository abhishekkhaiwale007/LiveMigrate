package com.livemigrate.model;

import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
    public class RecordMetadata {
        // Migration tracking
        private String source;
        private Instant migratedAt;
        private String migratedBy;
        private String migrationBatch;

        // Data quality metrics
        private double profileCompleteness;  // Percentage of filled optional fields
        private List<String> missingFields;  // Fields that could be filled but aren't
        private Map<String, Boolean> validationStatus;  // Results of various validation checks

        // Privacy and compliance
        private Map<String, Boolean> consentSettings;  // Different types of consent
        private Instant lastConsentUpdate;
        private String dataRegion;  // Geographic region for data sovereignty
        private List<String> appliedPolicies;  // Data retention/privacy policies

        // Communication preferences
        private Map<String, CommunicationChannel> communicationPreferences;

        // Access and modification history
        private List<AccessRecord> recentAccesses;
        private int accessCount;
        private Instant lastAccessTime;

        // Business metrics
        private String customerSegment;
        private double lifetimeValue;
        private String accountStatus;
        private List<String> subscribedServices;
    }
