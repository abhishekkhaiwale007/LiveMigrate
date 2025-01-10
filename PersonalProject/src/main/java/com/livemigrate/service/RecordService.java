package com.livemigrate.service;

import com.livemigrate.model.CustomerRecordV1;
import com.livemigrate.model.CustomerRecordV2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecordService {
    // Redis key prefixes for different record versions
    private static final String V1_KEY_PREFIX = "record:v1:";
    private static final String V2_KEY_PREFIX = "record:v2:";
    private static final String RECORD_IDS_KEY = "record:all_ids";

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Retrieves a V1 record by its ID.
     * This method is used for accessing records in the original format.
     */
    public Optional<CustomerRecordV1> getRecordV1(UUID id) {
        try {
            Object record = redisTemplate.opsForValue().get(V1_KEY_PREFIX + id.toString());
            return Optional.ofNullable(record)
                    .map(r -> (CustomerRecordV1) r);
        } catch (Exception e) {
            log.error("Error retrieving V1 record {}", id, e);
            throw new RuntimeException("Failed to retrieve V1 record", e);
        }
    }

    /**
     * Retrieves a V2 record by its ID.
     * This method is used for accessing records in the enhanced format.
     */
    public Optional<CustomerRecordV2> getRecordV2(UUID id) {
        try {
            Object record = redisTemplate.opsForValue().get(V2_KEY_PREFIX + id.toString());
            return Optional.ofNullable(record)
                    .map(r -> (CustomerRecordV2) r);
        } catch (Exception e) {
            log.error("Error retrieving V2 record {}", id, e);
            throw new RuntimeException("Failed to retrieve V2 record", e);
        }
    }

    /**
     * Saves a V1 record to the database.
     * This method handles both new records and updates to existing ones.
     */
    public void saveRecordV1(CustomerRecordV1 record) {
        try {
            String key = V1_KEY_PREFIX + record.getId().toString();
            redisTemplate.opsForValue().set(key, record);
            redisTemplate.opsForSet().add(RECORD_IDS_KEY, record.getId().toString());
        } catch (Exception e) {
            log.error("Error saving V1 record {}", record.getId(), e);
            throw new RuntimeException("Failed to save V1 record", e);
        }
    }

    /**
     * Saves a V2 record to the database.
     * This method handles both new records and updates to existing ones.
     */
    public void saveRecordV2(CustomerRecordV2 record) {
        try {
            String key = V2_KEY_PREFIX + record.getId().toString();
            redisTemplate.opsForValue().set(key, record);
            redisTemplate.opsForSet().add(RECORD_IDS_KEY, record.getId().toString());
        } catch (Exception e) {
            log.error("Error saving V2 record {}", record.getId(), e);
            throw new RuntimeException("Failed to save V2 record", e);
        }
    }

    /**
     * Saves multiple V2 records in a batch operation.
     * This method is optimized for bulk migrations.
     */
    public void saveRecordsV2(List<CustomerRecordV2> records) {
        try {
            Map<String, Object> batch = new HashMap<>();
            for (CustomerRecordV2 record : records) {
                String key = V2_KEY_PREFIX + record.getId().toString();
                batch.put(key, record);
                redisTemplate.opsForSet().add(RECORD_IDS_KEY, record.getId().toString());
            }
            redisTemplate.opsForValue().multiSet(batch);
        } catch (Exception e) {
            log.error("Error saving batch of V2 records", e);
            throw new RuntimeException("Failed to save V2 records batch", e);
        }
    }

    /**
     * Gets the total count of records in the system.
     * This is used for progress tracking during migration.
     */
    public long getTotalRecordCount() {
        try {
            Long size = redisTemplate.opsForSet().size(RECORD_IDS_KEY);
            return size != null ? size : 0L;
        } catch (Exception e) {
            log.error("Error getting total record count", e);
            throw new RuntimeException("Failed to get total record count", e);
        }
    }

    /**
     * Creates an iterator for processing records during migration.
     * This method supports resuming migration from a specific record ID.
     */
    public Iterator<CustomerRecordV1> getRecordIterator(Optional<UUID> startAfter) {
        try {
            // Get all record IDs
            Set<Object> allIds = redisTemplate.opsForSet().members(RECORD_IDS_KEY);
            if (allIds == null) {
                return Collections.emptyIterator();
            }

            // Convert to sorted list for consistent ordering
            List<String> sortedIds = allIds.stream()
                    .map(Object::toString)
                    .sorted()
                    .toList();

            // Find starting point if specified
            int startIndex = 0;
            if (startAfter.isPresent()) {
                String startId = startAfter.get().toString();
                startIndex = Collections.binarySearch(sortedIds, startId);
                startIndex = startIndex < 0 ? 0 : startIndex + 1;
            }

            // Create sublist from starting point
            List<String> remainingIds = sortedIds.subList(startIndex, sortedIds.size());

            // Return iterator that loads records on demand
            return new Iterator<>() {
                private final Iterator<String> idIterator = remainingIds.iterator();

                @Override
                public boolean hasNext() {
                    return idIterator.hasNext();
                }

                @Override
                public CustomerRecordV1 next() {
                    String nextId = idIterator.next();
                    return getRecordV1(UUID.fromString(nextId))
                            .orElseThrow(() -> new NoSuchElementException("Record not found: " + nextId));
                }
            };
        } catch (Exception e) {
            log.error("Error creating record iterator", e);
            throw new RuntimeException("Failed to create record iterator", e);
        }
    }

    /**
     * Deletes a record completely from both V1 and V2 storage.
     * This is typically used during cleanup operations.
     */
    public void deleteRecord(UUID id) {
        try {
            redisTemplate.delete(V1_KEY_PREFIX + id.toString());
            redisTemplate.delete(V2_KEY_PREFIX + id.toString());
            redisTemplate.opsForSet().remove(RECORD_IDS_KEY, id.toString());
        } catch (Exception e) {
            log.error("Error deleting record {}", id, e);
            throw new RuntimeException("Failed to delete record", e);
        }
    }
}