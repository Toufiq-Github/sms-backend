package com.spectrum.smsbackend.service;

import com.spectrum.smsbackend.model.UploadedFile;
import com.spectrum.smsbackend.model.User;
import com.spectrum.smsbackend.repository.FileRepository;
import com.spectrum.smsbackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FileService {

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private UserRepository userRepository;

    @Value("${file.upload.directory}")
    private String uploadDirectory;

    // Upload file
    public String uploadFile(MultipartFile file, String userEmail)
            throws IOException {

        String originalFileName = file.getOriginalFilename();
        String fileExtension = originalFileName
                .substring(originalFileName.lastIndexOf(".") + 1)
                .toLowerCase();

        if (!fileExtension.equals("pdf") &&
                !fileExtension.equals("xlsx") &&
                !fileExtension.equals("xls") &&
                !fileExtension.equals("txt")) {
            return "Invalid file type! Only PDF, Excel, and TXT allowed.";
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            return "File size exceeds 10MB limit!";
        }

        Path uploadPath = Paths.get(uploadDirectory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String fileName = System.currentTimeMillis()
                + "_" + originalFileName;
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath,
                StandardCopyOption.REPLACE_EXISTING);

        User user = userRepository.findByEmail(userEmail).orElseThrow();
        UploadedFile uploadedFile = new UploadedFile();
        uploadedFile.setFileName(originalFileName);
        uploadedFile.setFileType(fileExtension.toUpperCase());
        uploadedFile.setFileSize(file.getSize());
        uploadedFile.setFilePath(filePath.toString());
        uploadedFile.setUploadedBy(user);
        uploadedFile.setUploadedAt(LocalDateTime.now());

        fileRepository.save(uploadedFile);
        return "File uploaded successfully!";
    }

    // Get files — SCOPED to the logged-in user only
    public List<UploadedFile> getAllFiles(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow();
        return fileRepository.findByUploadedById(user.getId());
    }

    // Get file by id
    public UploadedFile getFileById(Long id) {
        return fileRepository.findById(id).orElseThrow();
    }

    // Delete file
    public String deleteFile(Long id) throws IOException {
        UploadedFile file = fileRepository.findById(id).orElseThrow();
        Files.deleteIfExists(Paths.get(file.getFilePath()));
        fileRepository.delete(file);
        return "File deleted successfully!";
    }
}