package com.ams.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "attendance_archives")
@CompoundIndexes({
    @CompoundIndex(
        name = "archive_student_period_unique_idx",
        def = "{'studentId': 1, 'dateRange': 1}",
        unique = true
    )
})
public class AttendanceArchive {

    @Id
    private String id;

    @Indexed
    private String studentId;

    private String dateRange;

    private long totalRecords;

    private long presentRecords;

    private long absentRecords;

    private double attendancePercentage;

    private Instant archivedAt;

    public AttendanceArchive() {
    }

    public AttendanceArchive(String studentId, String dateRange, long totalRecords,
                             long presentRecords, long absentRecords, double attendancePercentage,
                             Instant archivedAt) {
        this.studentId = studentId;
        this.dateRange = dateRange;
        this.totalRecords = totalRecords;
        this.presentRecords = presentRecords;
        this.absentRecords = absentRecords;
        this.attendancePercentage = attendancePercentage;
        this.archivedAt = archivedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getDateRange() {
        return dateRange;
    }

    public void setDateRange(String dateRange) {
        this.dateRange = dateRange;
    }

    public long getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(long totalRecords) {
        this.totalRecords = totalRecords;
    }

    public long getPresentRecords() {
        return presentRecords;
    }

    public void setPresentRecords(long presentRecords) {
        this.presentRecords = presentRecords;
    }

    public long getAbsentRecords() {
        return absentRecords;
    }

    public void setAbsentRecords(long absentRecords) {
        this.absentRecords = absentRecords;
    }

    public double getAttendancePercentage() {
        return attendancePercentage;
    }

    public void setAttendancePercentage(double attendancePercentage) {
        this.attendancePercentage = attendancePercentage;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(Instant archivedAt) {
        this.archivedAt = archivedAt;
    }
}
