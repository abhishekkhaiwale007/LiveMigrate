package com.livemigrate.component;

import com.livemigrate.model.*;
import com.livemigrate.service.RecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmartProxy {
    private final VersionSelector versionSelector;
    private final RecordService recordService;

    // Cache for handling in-flight requests during migration
    private final ConcurrentHashMap<UUID, Object> requestCache = new ConcurrentHashMap<>();

    /**
     * Retrieves a customer record, automatically handling version selection
     * and concurrent access during migration.
     */
    public Optional<Object> getRecord(UUID recordId) {
        // Check cache first for in-flight requests
        Object cachedRecord = requestCache.get(recordId);
        if (cachedRecord != null) {
            return Optional.of(cachedRecord);
        }

        try {
            RecordVersion version = versionSelector.getVersion(recordId);
            Optional<Object> record = switch (version) {
                case V1 -> recordService.getRecordV1(recordId).map(r -> (Object) r);
                case V2 -> recordService.getRecordV2(recordId).map(r -> (Object) r);
                case IN_MIGRATION -> handleMigrationStateRead(recordId);
            };

            // Cache the result for subsequent requests
            record.ifPresent(r -> requestCache.put(recordId, r));
            return record;

        } catch (Exception e) {
            log.error("Error retrieving record {}", recordId, e);
            throw new RuntimeException("Error retrieving record", e);
        }
    }

    /**
     * Updates a customer record, handling version-specific logic and
     * ensuring consistency during migration.
     */
    public void updateRecord(UUID recordId, Object recordData) {
        try {
            RecordVersion version = versionSelector.getVersion(recordId);

            switch (version) {
                case V1 -> {
                    if (!(recordData instanceof CustomerRecordV1)) {
                        throw new IllegalArgumentException("Invalid record format for V1");
                    }
                    recordService.saveRecordV1((CustomerRecordV1) recordData);
                }
                case V2 -> {
                    if (!(recordData instanceof CustomerRecordV2)) {
                        throw new IllegalArgumentException("Invalid record format for V2");
                    }
                    recordService.saveRecordV2((CustomerRecordV2) recordData);
                }
                case IN_MIGRATION -> handleMigrationStateWrite(recordId, recordData);
            }

            // Update cache
            requestCache.put(recordId, recordData);

        } catch (Exception e) {
            log.error("Error updating record {}", recordId, e);
            throw new RuntimeException("Error updating record", e);
        }
    }

    /**
     * Handles read operations for records that are currently being migrated.
     */
    private Optional<Object> handleMigrationStateRead(UUID recordId) {
        // Try V2 first, fall back to V1 if not found
        Optional<CustomerRecordV2> v2Record = recordService.getRecordV2(recordId);
        if (v2Record.isPresent()) {
            return Optional.of(v2Record.get());
        }

        return recordService.getRecordV1(recordId).map(r -> (Object) r);
    }

    /**
     * Handles write operations for records that are currently being migrated.
     */
    private void handleMigrationStateWrite(UUID recordId, Object recordData) {
        // During migration, write to both versions to maintain consistency
        if (recordData instanceof CustomerRecordV1 v1Record) {
            recordService.saveRecordV1(v1Record);
            // Also update V2 if it exists
            recordService.getRecordV2(recordId).ifPresent(v2Record -> {
                CustomerRecordV2 updatedV2 = updateV2FromV1(v2Record, v1Record);
                recordService.saveRecordV2(updatedV2);
            });
        } else if (recordData instanceof CustomerRecordV2 v2Record) {
            recordService.saveRecordV2(v2Record);
            // Create corresponding V1 record
            CustomerRecordV1 v1Record = createV1FromV2(v2Record);
            recordService.saveRecordV1(v1Record);
        }
    }

    /**
     * Updates a V2 record with changes from a V1 record while preserving V2-specific fields.
     */
    private CustomerRecordV2 updateV2FromV1(CustomerRecordV2 v2Record, CustomerRecordV1 v1Record) {
        v2Record.setCustomerData(v1Record.getCustomerData());
        v2Record.setChecksum(v1Record.getChecksum());
        v2Record.setLastModified(java.time.Instant.now());
        return v2Record;
    }

    /**
     * Creates a V1 record from a V2 record by extracting compatible fields.
     */
    private CustomerRecordV1 createV1FromV2(CustomerRecordV2 v2Record) {
        CustomerRecordV1 v1Record = new CustomerRecordV1();
        v1Record.setId(v2Record.getId());
        v1Record.setCreatedAt(v2Record.getCreatedAt());
        v1Record.setCustomerData(v2Record.getCustomerData());
        v1Record.setChecksum(v2Record.getChecksum());
        return v1Record;
    }

    /**
     * Clears the request cache, typically called during migration state changes.
     */
    public void clearCache() {
        requestCache.clear();
    }
}