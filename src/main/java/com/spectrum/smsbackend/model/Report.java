package com.spectrum.smsbackend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String reportName;

    @Column(nullable = false)
    private String reportType;

    @ManyToOne
    @JoinColumn(name = "generated_by")
    private User generatedBy;

    @Column
    private String filePath;

    @Column
    private LocalDateTime createdAt = LocalDateTime.now();
}