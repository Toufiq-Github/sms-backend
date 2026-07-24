package com.spectrum.smsbackend.repository;

import com.spectrum.smsbackend.model.DatasetFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DatasetFileRepository
        extends JpaRepository<DatasetFile, Long> {

    List<DatasetFile> findByUploadedById(Long userId);

    Optional<DatasetFile> findByFileNameAndUploadedById(
            String fileName, Long userId);

    List<DatasetFile>
    findByUploadedByIdAndUploadYearAndUploadMonth(
            Long userId, Integer year, Integer month);

    List<DatasetFile>
    findByUploadedByIdAndUploadYearAndUploadMonthAndUploadDay(
            Long userId, Integer year, Integer month,
            Integer day);
}