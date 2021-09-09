package io.mosip.registration.repositories;

import io.mosip.kernel.core.dataaccess.spi.repository.BaseRepository;
import io.mosip.registration.entity.FileSignature;

import java.util.Optional;

public interface FileSignatureRepository extends BaseRepository<FileSignature, String> {

    Optional<FileSignature> findByFileName(String fileName);
}
