package com.spectrum.smsbackend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "dataset_files")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatasetFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String fileType;

    @Column(nullable = false)
    private String dataCategory;

    @ManyToOne
    @JoinColumn(name = "schema_id", nullable = false)
    private DatasetSchema schema;

    // Denormalized copy for fast reads
    @Column(nullable = false)
    private String tableName;

    @Column
    private Integer totalRows;

    @ManyToOne
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    @Column
    private Integer uploadYear;

    @Column
    private Integer uploadMonth;

    @Column
    private Integer uploadDay;

    @Column
    private String eodDate;

    @Column
    private LocalDateTime createdAt = LocalDateTime.now();
}