package com.civis.api;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

interface OfficerRepository extends JpaRepository<Officer,String> { boolean existsByEmailIgnoreCase(String email); }
interface DefenseRecordRepository extends JpaRepository<DefenseRecord,String> {}
interface CitizenRepository extends JpaRepository<Citizen,String> {}
interface StatusHistoryRepository extends JpaRepository<StatusHistory,Long> {}
interface AuthSessionRepository extends JpaRepository<AuthSession,String> {
 @org.springframework.data.jpa.repository.EntityGraph(attributePaths="officer")
 Optional<AuthSession> findById(String id);
 void deleteByOfficerId(String id);
}
interface LookupAuditRepository extends JpaRepository<LookupAudit,Long> {
 org.springframework.data.domain.Page<LookupAudit> findByOfficerId(String officerId,org.springframework.data.domain.Pageable pageable);
}

interface CitizenRegistrationAuditRepository extends JpaRepository<CitizenRegistrationAudit,Long> {}
