package com.spectrum.smsbackend.repository;

import com.spectrum.smsbackend.model.UploadedFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FileRepository extends JpaRepository<UploadedFile, Long> {

    List<UploadedFile> findByUploadedById(Long userId);

    List<UploadedFile> findByFileType(String fileType);

    long countByUploadedById(Long userId);
}