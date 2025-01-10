package com.livemigrate.component;

import com.livemigrate.model.*;
import com.livemigrate.service.RecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class MigrationCoordinator {
    // Batch size for processing records
    private static final int BATCH_SIZE = 10;

    private static final long BATCH_DELAY = 3000;

    // Add delay between individual records (in milliseconds)
    private static final long RECORD_DELAY = 300;

    private final StateTracker stateTracker;
    private final VersionSelector versionSelector;
    private final RecordService recordService;

    // Flag to control migration process
    private final AtomicBoolean migrationInProgress = new AtomicBoolean(false);

    /**
     * Initiates the migration process. This method validates preconditions
     * and sets up the initial state for migration.
     */
    public void startMigration() {
        if (!migrationInProgress.compareAndSet(false, true)) {
            throw new IllegalStateException("Migration is already in progress");
        }

        try {
            stateTracker.setState(MigrationState.PREPARING);
            log.info("Starting migration process");
            performMigration();
        } catch (Exception e) {
            log.error("Error during migration start", e);
            stateTracker.setState(MigrationState.ERROR);
            migrationInProgress.set(false);
            throw e;
        }
    }

    /**
     * Performs the actual migration of records from V1 to V2 format.
     * This method handles the core transformation logic and progress tracking.
     */
    private void performMigration() {
        try {
            stateTracker.setState(MigrationState.MIGRATING);
            long totalRecords = recordService.getTotalRecordCount();
            long processedRecords = 0;

            log.info("Starting migration with {} total records", totalRecords);

            Optional<UUID> lastProcessedId = stateTracker.getLastCheckpoint();
            if (lastProcessedId.isPresent()) {
                log.info("Resuming migration from checkpoint: {}", lastProcessedId.get());
                // Count how many records we've already processed
                processedRecords = versionSelector.getProcessedCount();
                log.info("Found {} already processed records", processedRecords);
            } else {
                log.info("Starting fresh migration");
            }

            Iterator<CustomerRecordV1> recordIterator = recordService.getRecordIterator(lastProcessedId);

            while (recordIterator.hasNext() && migrationInProgress.get()) {
                List<CustomerRecordV1> batch = getNextBatch(recordIterator);
                List<CustomerRecordV2> migratedBatch = new ArrayList<>();

                // Process each record in the batch with a delay
                for (CustomerRecordV1 record : batch) {
                    CustomerRecordV2 migratedRecord = migrateRecord(record);
                    migratedBatch.add(migratedRecord);
                    log.info("Migrated record: {}", record.getId());

                    // Add delay between individual records
                    Thread.sleep(RECORD_DELAY);
                }

                recordService.saveRecordsV2(migratedBatch);
                migratedBatch.forEach(record -> versionSelector.markAsMigrated(record.getId()));

                processedRecords += batch.size();
                double progress = (processedRecords * 100.0) / totalRecords;
                stateTracker.updateProgress(processedRecords, totalRecords);

                log.info("Processed batch of {} records. Progress: {}/{} ({:.2f}%)",
                        batch.size(), processedRecords, totalRecords, progress);

                if (!batch.isEmpty()) {
                    stateTracker.saveCheckpoint(batch.get(batch.size() - 1).getId());
                }

                // Add delay between batches
                Thread.sleep(BATCH_DELAY);
            }

            if (migrationInProgress.get()) {
                validateMigration();
            }

        } catch (InterruptedException e) {
            log.warn("Migration was interrupted", e);
            stateTracker.setState(MigrationState.PAUSED);
        } catch (Exception e) {
            log.error("Error during migration process", e);
            stateTracker.setState(MigrationState.ERROR);
            throw e;
        }
    }


    /**
     * Retrieves the next batch of records for processing.
     */
    private List<CustomerRecordV1> getNextBatch(Iterator<CustomerRecordV1> iterator) {
        List<CustomerRecordV1> batch = new ArrayList<>();
        while (iterator.hasNext() && batch.size() < BATCH_SIZE) {
            batch.add(iterator.next());
        }
        return batch;
    }

    /**
     * Transforms a batch of V1 records to V2 format.
     */
    private List<CustomerRecordV2> migrateBatch(List<CustomerRecordV1> batch) {
        return batch.stream()
                .map(this::migrateRecord)
                .toList();
    }

    /**
     * Transforms a single record from V1 to V2 format.
     * This method handles the actual schema evolution logic.
     */
    private CustomerRecordV2 migrateRecord(CustomerRecordV1 v1Record) {
        CustomerRecordV2 v2Record = new CustomerRecordV2();
        Instant now = Instant.now();

        // Copy existing fields
        v2Record.setId(v1Record.getId());
        v2Record.setCreatedAt(v1Record.getCreatedAt());
        v2Record.setCustomerData(v1Record.getCustomerData());
        v2Record.setChecksum(v1Record.getChecksum());

        // Set basic V2 fields
        v2Record.setVersion((short) 2);
        v2Record.setLastModified(now);

        // Create and populate metadata
        RecordMetadata metadata = new RecordMetadata();

        // Migration tracking
        metadata.setSource("migration");
        metadata.setMigratedAt(now);
        metadata.setMigratedBy("system-migration-v1-to-v2");
        metadata.setMigrationBatch(UUID.randomUUID().toString());  // Unique batch identifier

        // Data quality assessment
        metadata.setProfileCompleteness(calculateProfileCompleteness(v1Record));
        metadata.setMissingFields(identifyMissingFields(v1Record));
        metadata.setValidationStatus(performDataValidation(v1Record));

        // Initialize privacy and compliance settings
        metadata.setConsentSettings(initializeConsentSettings());
        metadata.setLastConsentUpdate(v1Record.getCreatedAt());  // Use creation date as initial consent
        metadata.setDataRegion("default-region");
        metadata.setAppliedPolicies(List.of("standard-retention", "gdpr-compliance"));

        // Set up communication preferences
        metadata.setCommunicationPreferences(initializeCommunicationPreferences(v1Record));

        // Initialize access history
        metadata.setRecentAccesses(new ArrayList<>());
        metadata.setAccessCount(0);
        metadata.setLastAccessTime(now);

        // Analyze and set business metrics
        metadata.setCustomerSegment(determineCustomerSegment(v1Record));
        metadata.setLifetimeValue(0.0);  // Initialize with default
        metadata.setAccountStatus("active");
        metadata.setSubscribedServices(new ArrayList<>());

        v2Record.setMetadata(metadata);
        return v2Record;
    }

    private double calculateProfileCompleteness(CustomerRecordV1 record) {
        int totalFields = 3;  // name, email, phone
        int filledFields = 0;

        CustomerData data = record.getCustomerData();
        if (data.getName() != null && !data.getName().isEmpty()) filledFields++;
        if (data.getEmail() != null && !data.getEmail().isEmpty()) filledFields++;
        if (data.getPhone() != null && !data.getPhone().isEmpty()) filledFields++;

        return (filledFields * 100.0) / totalFields;
    }

    private List<String> identifyMissingFields(CustomerRecordV1 record) {
        List<String> missing = new ArrayList<>();
        CustomerData data = record.getCustomerData();

        if (data.getName() == null || data.getName().isEmpty()) missing.add("name");
        if (data.getEmail() == null || data.getEmail().isEmpty()) missing.add("email");
        if (data.getPhone() == null || data.getPhone().isEmpty()) missing.add("phone");

        return missing;
    }

    private Map<String, Boolean> performDataValidation(CustomerRecordV1 record) {
        Map<String, Boolean> validations = new HashMap<>();
        CustomerData data = record.getCustomerData();

        // Email validation
        validations.put("validEmail",
                data.getEmail() != null && data.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$"));

        // Phone number validation
        validations.put("validPhone",
                data.getPhone() != null && data.getPhone().matches("\\(\\d{3}\\) \\d{3}-\\d{4}"));

        // Name validation
        validations.put("validName",
                data.getName() != null && data.getName().split("\\s+").length >= 2);

        return validations;
    }

    private Map<String, Boolean> initializeConsentSettings() {
        Map<String, Boolean> consent = new HashMap<>();
        consent.put("marketing", false);
        consent.put("analytics", true);
        consent.put("thirdParty", false);
        return consent;
    }

    private Map<String, CommunicationChannel> initializeCommunicationPreferences(CustomerRecordV1 record) {
        Map<String, CommunicationChannel> prefs = new HashMap<>();

        // Initialize email preferences
        CommunicationChannel email = new CommunicationChannel();
        email.setEnabled(true);
        email.setPreferredTime("morning");
        email.setFrequency("weekly");
        email.setLastContact(record.getCreatedAt());
        email.setHasOptedOut(false);
        prefs.put("email", email);

        // Initialize SMS preferences
        CommunicationChannel sms = new CommunicationChannel();
        sms.setEnabled(true);
        sms.setPreferredTime("afternoon");
        sms.setFrequency("weekly");
        sms.setLastContact(record.getCreatedAt());
        sms.setHasOptedOut(false);
        prefs.put("sms", sms);

        return prefs;
    }

    private String determineCustomerSegment(CustomerRecordV1 record) {
        // Simple segmentation based on email domain
        String email = record.getCustomerData().getEmail().toLowerCase();
        if (email.endsWith("gmail.com")) return "consumer";
        if (email.endsWith("yahoo.com")) return "consumer";
        return "business";
    }


    /**
     * Validates the completed migration by performing consistency checks.
     */
    private void validateMigration() {
        stateTracker.setState(MigrationState.VALIDATING);

        try {
            // Perform validation checks
            boolean validationSuccess = performValidationChecks();

            if (validationSuccess) {
                stateTracker.setState(MigrationState.SWITCHING);
                completeMigration();
            } else {
                stateTracker.setState(MigrationState.ERROR);
                throw new RuntimeException("Migration validation failed");
            }

        } catch (Exception e) {
            log.error("Error during migration validation", e);
            stateTracker.setState(MigrationState.ERROR);
            throw e;
        }
    }

    /**
     * Performs validation checks to ensure data consistency.
     */
    private boolean performValidationChecks() {
        // In a real implementation, this would include:
        // 1. Record count verification
        // 2. Data integrity checks
        // 3. Sample record comparisons
        // 4. Performance metrics validation
        return true; // Simplified for prototype
    }

    /**
     * Completes the migration process and performs cleanup.
     */
    private void completeMigration() {
        stateTracker.setState(MigrationState.COMPLETED);
        migrationInProgress.set(false);
        log.info("Migration process completed successfully");
    }

    /**
     * Pauses the ongoing migration process.
     */
    public void pauseMigration() {
        if (migrationInProgress.get()) {
            migrationInProgress.set(false);
            stateTracker.setState(MigrationState.PAUSED);
            log.info("Migration process paused");
        }
    }

    /**
     * Resumes a paused migration process.
     */
    public void resumeMigration() {
        if (stateTracker.getState() == MigrationState.PAUSED) {
            migrationInProgress.set(true);
            performMigration();
            log.info("Migration process resumed");
        }
    }
}