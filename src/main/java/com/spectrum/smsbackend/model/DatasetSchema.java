package com.spectrum.smsbackend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "dataset_schemas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatasetSchema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // SHA-256 hash of category + normalized column names
    @Column(nullable = false, unique = true)
    private String schemaHash;

    // Physical Postgres table name holding this structure's data
    @Column(nullable = false)
    private String tableName;

    @Column(nullable = false)
    private String dataCategory;

    // Original column names, comma-separated, in order
    @Column(columnDefinition = "TEXT", nullable = false)
    private String columnDefinitions;

    @Column
    private LocalDateTime createdAt = LocalDateTime.now();
}