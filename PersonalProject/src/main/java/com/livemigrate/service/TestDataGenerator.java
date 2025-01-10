package com.livemigrate.service;

import com.livemigrate.model.CustomerData;
import com.livemigrate.model.CustomerRecordV1;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class TestDataGenerator {
    private final RecordService recordService;

    // Lists for generating realistic test data
    private static final List<String> FIRST_NAMES = Arrays.asList(
            "James", "Mary", "John", "Patricia", "Robert", "Jennifer", "Michael", "Linda",
            "William", "Elizabeth", "David", "Barbara", "Richard", "Susan", "Joseph", "Jessica"
    );

    private static final List<String> LAST_NAMES = Arrays.asList(
            "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis",
            "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson"
    );

    private static final List<String> EMAIL_DOMAINS = Arrays.asList(
            "gmail.com", "yahoo.com", "hotmail.com", "outlook.com", "example.com"
    );

    /**
     * Generates and stores a specified number of test records.
     * This method creates realistic-looking customer data for testing purposes.
     *
     * @param count The number of test records to generate
     * @return The list of generated record IDs
     */
    public List<UUID> generateTestRecords(int count) {
        List<UUID> generatedIds = new ArrayList<>();
        List<CustomerRecordV1> batch = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            CustomerRecordV1 record = generateRandomRecord();
            batch.add(record);
            generatedIds.add(record.getId());

            // Process in batches of 100 to avoid memory issues
            if (batch.size() >= 100 || i == count - 1) {
                saveBatch(batch);
                batch.clear();
            }
        }

        return generatedIds;
    }

    /**
     * Generates a single random customer record.
     * This method creates realistic-looking customer data with proper relationships
     * between fields (e.g., matching email to name).
     */
    private CustomerRecordV1 generateRandomRecord() {
        // Generate a random customer name
        String firstName = getRandomElement(FIRST_NAMES);
        String lastName = getRandomElement(LAST_NAMES);
        String fullName = firstName + " " + lastName;

        // Create email based on name
        String email = generateEmail(firstName, lastName);

        // Generate phone number
        String phone = generatePhoneNumber();

        // Create the customer data object
        CustomerData customerData = new CustomerData();
        customerData.setName(fullName);
        customerData.setEmail(email);
        customerData.setPhone(phone);

        // Create the complete record
        CustomerRecordV1 record = new CustomerRecordV1();
        record.setId(UUID.randomUUID());
        record.setCreatedAt(generateRandomTimestamp());
        record.setCustomerData(customerData);
        record.setChecksum(calculateChecksum(customerData));

        return record;
    }

    /**
     * Generates a realistic email address based on the customer's name.
     */
    private String generateEmail(String firstName, String lastName) {
        String domain = getRandomElement(EMAIL_DOMAINS);

        // Create email variations
        List<String> emailPatterns = Arrays.asList(
                firstName.toLowerCase() + "." + lastName.toLowerCase(),
                firstName.toLowerCase() + lastName.toLowerCase(),
                firstName.toLowerCase().charAt(0) + lastName.toLowerCase(),
                lastName.toLowerCase() + firstName.toLowerCase().charAt(0)
        );

        String emailPattern = getRandomElement(emailPatterns);
        return emailPattern + "@" + domain;
    }

    /**
     * Generates a realistic-looking phone number.
     */
    private String generatePhoneNumber() {
        // Format: (XXX) XXX-XXXX
        return String.format("(%03d) %03d-%04d",
                ThreadLocalRandom.current().nextInt(100, 999),
                ThreadLocalRandom.current().nextInt(100, 999),
                ThreadLocalRandom.current().nextInt(0, 9999)
        );
    }

    /**
     * Generates a random timestamp within the last year.
     */
    private Instant generateRandomTimestamp() {
        Instant now = Instant.now();
        long daysToSubtract = ThreadLocalRandom.current().nextLong(365);
        return now.minus(daysToSubtract, ChronoUnit.DAYS);
    }

    /**
     * Calculates a simple checksum for the customer data.
     * In a real application, this would use a more sophisticated algorithm.
     */
    private int calculateChecksum(CustomerData customerData) {
        String concatenated = customerData.getName() +
                customerData.getEmail() +
                customerData.getPhone();
        return concatenated.hashCode();
    }

    /**
     * Helper method to get a random element from a list.
     */
    private <T> T getRandomElement(List<T> list) {
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

    /**
     * Saves a batch of records using the record service.
     */
    private void saveBatch(List<CustomerRecordV1> batch) {
        for (CustomerRecordV1 record : batch) {
            recordService.saveRecordV1(record);
        }
    }
}