package com.insurance.gw.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insurance.gw.model.SubmissionData;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/** Loads named customer profiles from src/test/resources/testdata/customers.json (data-driven testing). */
public final class TestDataReader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TestDataReader() {
    }

    /** Returns a NEW object every call so scenarios never share (and accidentally mutate) test data. */
    public static SubmissionData profile(String name) {
        try (InputStream in = TestDataReader.class.getClassLoader().getResourceAsStream("testdata/customers.json")) {
            Map<String, SubmissionData> all = MAPPER.readValue(in, new TypeReference<Map<String, SubmissionData>>() {
            });
            SubmissionData data = all.get(name);
            if (data == null) {
                throw new IllegalArgumentException("Unknown test-data profile '" + name + "'. Available: " + all.keySet());
            }
            return data;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read customers.json", e);
        }
    }
}
