package com.refind.repository;

import com.refind.entity.Claim;
import com.refind.entity.ClaimStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {

    List<Claim> findByReportIdOrderByCreatedAtDesc(Long reportId);

    List<Claim> findByClaimantIdOrderByCreatedAtDesc(Long claimantId);

    List<Claim> findByReportUserIdOrderByCreatedAtDesc(Long reportUserId);

    boolean existsByReportIdAndClaimantIdAndStatus(Long reportId, Long claimantId, ClaimStatus status);

    long countByStatus(ClaimStatus status);

    Page<Claim> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
