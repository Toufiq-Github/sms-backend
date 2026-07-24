package com.spectrum.smsbackend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "data_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // What type of data this is
    @Column(nullable = false)
    private String dataCategory;

    // Source file name
    @Column(nullable = false)
    private String sourceFileName;

    // Source file type (PDF, EXCEL, CSV)
    @Column(nullable = false)
    private String sourceFileType;

    // Row number from source file
    @Column
    private Integer rowNumber;

    // Column name from source file
    @Column
    private String columnName;

    // Actual value stored as string
    @Column(columnDefinition = "TEXT")
    private String value;

    // Detected data type (STRING, INTEGER, FLOAT)
    @Column
    private String dataType;

    // Date metadata for filtering
    @Column
    private Integer uploadYear;

    @Column
    private Integer uploadMonth;

    @Column
    private Integer uploadDay;

    // EOD date selected by user during upload
    @Column
    private String eodDate;

    // Who uploaded this
    @ManyToOne
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    @Column
    private LocalDateTime createdAt = LocalDateTime.now();
}