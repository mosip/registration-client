package io.mosip.registration.repositories;

import io.mosip.registration.entity.IdentitySchema;
import io.mosip.registration.entity.ProcessSpec;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProcessSpecRepository extends JpaRepository<ProcessSpec, String> {

    List<ProcessSpec> findAllByIdVersionAndIsActiveTrueOrderByOrderNumAsc(double idVersion);

    ProcessSpec findByIdAndIdVersionAndIsActiveTrue(String processId, double idVersion);
}
