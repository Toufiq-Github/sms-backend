package com.spectrum.smsbackend.repository;

import com.spectrum.smsbackend.model.DataRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Repository
public interface DataRecordRepository
        extends JpaRepository<DataRecord, Long> {

    List<DataRecord> findByDataCategory(
            String dataCategory);

    List<DataRecord> findBySourceFileName(
            String sourceFileName);

    List<DataRecord> findByUploadedById(
            Long userId);

    // Distinct files for a specific user
    @Query("SELECT DISTINCT d.sourceFileName " +
            "FROM DataRecord d " +
            "WHERE d.uploadedBy.id = :userId")
    List<String> findDistinctFilesByUser(
            @Param("userId") Long userId);

    // All distinct files (admin)
    @Query("SELECT DISTINCT d.sourceFileName " +
            "FROM DataRecord d")
    List<String> findAllDistinctFiles();

    // Delete by filename (admin)
    @Modifying
    @Transactional
    @Query("DELETE FROM DataRecord d " +
            "WHERE d.sourceFileName = :fileName")
    void deleteBySourceFileName(
            @Param("fileName") String fileName);

    // Delete by filename + user (user own files)
    @Modifying
    @Transactional
    @Query("DELETE FROM DataRecord d " +
            "WHERE d.sourceFileName = :fileName " +
            "AND d.uploadedBy.id = :userId")
    void deleteBySourceFileNameAndUploadedById(
            @Param("fileName") String fileName,
            @Param("userId") Long userId);

    // Filter by year + month for user
    @Query("SELECT DISTINCT d.sourceFileName " +
            "FROM DataRecord d " +
            "WHERE d.uploadedBy.id = :userId " +
            "AND d.uploadYear = :year " +
            "AND d.uploadMonth = :month")
    List<String> findFilesByUserAndYearMonth(
            @Param("userId") Long userId,
            @Param("year") Integer year,
            @Param("month") Integer month);

    // Filter by year + month + day for user
    @Query("SELECT DISTINCT d.sourceFileName " +
            "FROM DataRecord d " +
            "WHERE d.uploadedBy.id = :userId " +
            "AND d.uploadYear = :year " +
            "AND d.uploadMonth = :month " +
            "AND d.uploadDay = :day")
    List<String> findFilesByUserAndDate(
            @Param("userId") Long userId,
            @Param("year") Integer year,
            @Param("month") Integer month,
            @Param("day") Integer day);

    // Filter by year + month for admin
    @Query("SELECT DISTINCT d.sourceFileName " +
            "FROM DataRecord d " +
            "WHERE d.uploadYear = :year " +
            "AND d.uploadMonth = :month")
    List<String> findAllFilesByYearMonth(
            @Param("year") Integer year,
            @Param("month") Integer month);

    // Filter by year for user
    @Query("SELECT DISTINCT d.sourceFileName " +
            "FROM DataRecord d " +
            "WHERE d.uploadedBy.id = :userId " +
            "AND d.uploadYear = :year")
    List<String> findFilesByUserAndYear(
            @Param("userId") Long userId,
            @Param("year") Integer year);

    // Filter by year + month + fileType for user
    @Query("SELECT DISTINCT d.sourceFileName " +
            "FROM DataRecord d " +
            "WHERE d.uploadedBy.id = :userId " +
            "AND d.uploadYear = :year " +
            "AND d.uploadMonth = :month " +
            "AND d.sourceFileType = :fileType")
    List<String> findFilesByUserYearMonthType(
            @Param("userId") Long userId,
            @Param("year") Integer year,
            @Param("month") Integer month,
            @Param("fileType") String fileType);

    // Check file belongs to user
    @Query("SELECT COUNT(d) > 0 " +
            "FROM DataRecord d " +
            "WHERE d.sourceFileName = :fileName " +
            "AND d.uploadedBy.id = :userId")
    boolean existsByFileNameAndUserId(
            @Param("fileName") String fileName,
            @Param("userId") Long userId);

    // Get records for a specific file belonging to a specific user
    List<DataRecord> findBySourceFileNameAndUploadedById(
            String sourceFileName, Long userId);

    // Count files for a specific user (used internally)
    long countBySourceFileNameAndUploadedById(
            String sourceFileName, Long userId);

    // Bulk delete all data records for a user (used when deleting a user account)
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("DELETE FROM DataRecord d WHERE d.uploadedBy.id = :userId")
    void deleteByUploadedById(@Param("userId") Long userId);
}