package com.giftnova.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Returned after a CSV upload.
 * Tells the UI how many rows succeeded and lists all validation errors with row numbers.
 */
public class CsvUploadResult {

    private int successCount;
    private final List<String> errors = new ArrayList<>();

    public void incrementSuccess() { successCount++; }
    public void addError(String error) { errors.add(error); }

    public int getSuccessCount() { return successCount; }
    public List<String> getErrors() { return errors; }
    public boolean hasErrors() { return !errors.isEmpty(); }
}
