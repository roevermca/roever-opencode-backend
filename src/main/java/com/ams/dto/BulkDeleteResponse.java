package com.ams.dto;

import java.util.ArrayList;
import java.util.List;

public class BulkDeleteResponse {

    private int totalRequested;
    private int deletedCount;
    private int failedCount;
    private List<String> errors = new ArrayList<>();

    public BulkDeleteResponse() {
    }

    public BulkDeleteResponse(int totalRequested, int deletedCount, int failedCount, List<String> errors) {
        this.totalRequested = totalRequested;
        this.deletedCount = deletedCount;
        this.failedCount = failedCount;
        this.errors = errors != null ? errors : new ArrayList<>();
    }

    public int getTotalRequested() {
        return totalRequested;
    }

    public void setTotalRequested(int totalRequested) {
        this.totalRequested = totalRequested;
    }

    public int getDeletedCount() {
        return deletedCount;
    }

    public void setDeletedCount(int deletedCount) {
        this.deletedCount = deletedCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors != null ? errors : new ArrayList<>();
    }
}
