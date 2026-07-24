package com.spectrum.smsbackend.repository;

import com.spectrum.smsbackend.model.DatasetSchema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DatasetSchemaRepository
        extends JpaRepository<DatasetSchema, Long> {

    Optional<DatasetSchema> findBySchemaHash(String schemaHash);
}