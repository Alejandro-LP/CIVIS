package com.civis.api;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity @Table(name="officers")
class Officer {
 @Id @Column(length=25) String id;
 String firstName, lastName, forceName, rankName, forceId, email, passwordHash;
 @Column(nullable=false) String role="OFFICER";
 @Column(nullable=false) boolean active=true;
 protected Officer() {}
}
@Entity @Table(name="defense_registry", uniqueConstraints=@UniqueConstraint(columnNames={"forceId"}))
class DefenseRecord {
 @Id String documentId;
 String forceName, rankName, forceId, email;
 boolean active=true;
 protected DefenseRecord() {}
}
@Entity @Table(name="citizens")
class Citizen {
 @Id String documentId;
 String firstName, lastName, birthPlace, issuePlace, rh, bloodGroup;
 LocalDate birthDate, issueDate;
 Integer heightCm;
 @Column(nullable=false) boolean required=false;
 protected Citizen() {}
}
@Entity @Table(name="auth_sessions")
class AuthSession {
 @Id @Column(length=64) String tokenHash;
 @ManyToOne(fetch=FetchType.LAZY) Officer officer;
 Instant expiresAt;
 protected AuthSession() {}
}
@Entity @Table(name="lookup_audit")
class LookupAudit {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
 String officerId, citizenId, outcome;
 Instant occurredAt;
 protected LookupAudit() {}
}
@Entity @Table(name="status_history")
class StatusHistory {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
 String officerId, citizenId;
 boolean previousRequired, newRequired;
 Instant occurredAt;
 protected StatusHistory() {}
}
