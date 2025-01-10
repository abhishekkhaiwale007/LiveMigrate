package com.livemigrate.controller;

import com.livemigrate.component.MigrationCoordinator;
import com.livemigrate.component.SmartProxy;
import com.livemigrate.component.StateTracker;
import com.livemigrate.model.CustomerRecordV1;
import com.livemigrate.model.CustomerRecordV2;
import com.livemigrate.model.MigrationState;
import com.livemigrate.service.TestDataGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MigrationController {

    private final MigrationCoordinator migrationCoordinator;
    private final StateTracker stateTracker;
    private final SmartProxy smartProxy;
    private final TestDataGenerator testDataGenerator;

    @PostMapping("/migration/start")
    public ResponseEntity<Map<String, String>> startMigration() {
        try {
            migrationCoordinator.startMigration();
            return ResponseEntity.ok(Map.of("message", "Migration started successfully"));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/migration/pause")
    public ResponseEntity<Map<String, String>> pauseMigration() {
        try {
            migrationCoordinator.pauseMigration();
            return ResponseEntity.ok(Map.of("message", "Migration paused successfully"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to pause migration: " + e.getMessage()));
        }
    }

    @PostMapping("/migration/resume")
    public ResponseEntity<Map<String, String>> resumeMigration() {
        try {
            migrationCoordinator.resumeMigration();
            return ResponseEntity.ok(Map.of("message", "Migration resumed successfully"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to resume migration: " + e.getMessage()));
        }
    }

    @GetMapping("/migration/status")
    public ResponseEntity<MigrationStatus> getMigrationStatus() {
        MigrationStatus status = new MigrationStatus();
        status.setState(stateTracker.getState());
        status.setProgress(stateTracker.getProgress());
        return ResponseEntity.ok(status);
    }

    @GetMapping("/records/{id}")
    public ResponseEntity<Object> getRecord(@PathVariable UUID id) {
        return smartProxy.getRecord(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/records/v1/{id}")
    public ResponseEntity<Map<String, String>> updateRecordV1(
            @PathVariable UUID id,
            @RequestBody CustomerRecordV1 record) {
        try {
            if (!id.equals(record.getId())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "ID in URL does not match record ID"));
            }
            smartProxy.updateRecord(id, record);
            return ResponseEntity.ok(Map.of("message", "Record updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to update record: " + e.getMessage()));
        }
    }

    @PutMapping("/records/v2/{id}")
    public ResponseEntity<Map<String, String>> updateRecordV2(
            @PathVariable UUID id,
            @RequestBody CustomerRecordV2 record) {
        try {
            if (!id.equals(record.getId())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "ID in URL does not match record ID"));
            }
            smartProxy.updateRecord(id, record);
            return ResponseEntity.ok(Map.of("message", "Record updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to update record: " + e.getMessage()));
        }
    }

    @PostMapping("/test-data/generate")
    public ResponseEntity<Map<String, Object>> generateTestData(
            @RequestParam(defaultValue = "100") int count) {
        try {
            if (count <= 0 || count > 10000) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Count must be between 1 and 10000");
                error.put("providedCount", count);
                return ResponseEntity.badRequest().body(error);
            }

            List<UUID> generatedIds = testDataGenerator.generateTestRecords(count);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Successfully generated test data");
            response.put("recordCount", generatedIds.size());
            response.put("sampleIds", generatedIds.stream().limit(5).toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "Failed to generate test data",
                            "message", e.getMessage()
                    ));
        }
    }
}

// Static nested class for migration status response
class MigrationStatus {
    private MigrationState state;
    private double progress;

    // Getters and setters
    public MigrationState getState() {
        return state;
    }

    public void setState(MigrationState state) {
        this.state = state;
    }

    public double getProgress() {
        return progress;
    }

    public void setProgress(double progress) {
        this.progress = progress;
    }
}