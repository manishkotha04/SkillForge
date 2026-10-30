package com.skillforge.skillforge.repository;

import com.skillforge.skillforge.entity.OwnershipTransfer;
import com.skillforge.skillforge.enums.OwnershipTransferStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OwnershipTransferRepository
        extends JpaRepository<OwnershipTransfer, Long> {

    boolean existsByProject_IdAndStatus(
            Long projectId,
            OwnershipTransferStatus status);

    @EntityGraph(attributePaths = {"project", "requestedBy"})
    List<OwnershipTransfer> findByNominatedUser_IdAndStatus(
            Long userId,
            OwnershipTransferStatus status);

    @EntityGraph(attributePaths = {"project", "nominatedUser"})
    Optional<OwnershipTransfer> findById(Long id);
}