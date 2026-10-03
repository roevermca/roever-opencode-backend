package com.ams.dto;

import java.util.ArrayList;
import java.util.List;

public class BulkStudentImportResponse {

    private int totalRequested;
    private int importedCount;
    private int skippedCount;
    private List<String> errors = new ArrayList<>();
    private List<StudentResponse> importedStudents = new ArrayList<>();

    public BulkStudentImportResponse() {
    }

    public BulkStudentImportResponse(int totalRequested, int importedCount, int skippedCount,
                                     List<String> errors, List<StudentResponse> importedStudents) {
        this.totalRequested = totalRequested;
        this.importedCount = importedCount;
        this.skippedCount = skippedCount;
        this.errors = errors != null ? errors : new ArrayList<>();
        this.importedStudents = importedStudents != null ? importedStudents : new ArrayList<>();
    }

    public int getTotalRequested() {
        return totalRequested;
    }

    public void setTotalRequested(int totalRequested) {
        this.totalRequested = totalRequested;
    }

    public int getImportedCount() {
        return importedCount;
    }

    public void setImportedCount(int importedCount) {
        this.importedCount = importedCount;
    }

    public int getSkippedCount() {
        return skippedCount;
    }

    public void setSkippedCount(int skippedCount) {
        this.skippedCount = skippedCount;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public List<StudentResponse> getImportedStudents() {
        return importedStudents;
    }

    public void setImportedStudents(List<StudentResponse> importedStudents) {
        this.importedStudents = importedStudents;
    }
}
