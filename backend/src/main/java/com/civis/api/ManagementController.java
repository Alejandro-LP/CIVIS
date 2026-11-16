package com.civis.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api")
class ManagementController {
 private final ApiService auth;
 private final OfficerRepository officers;
 private final DefenseRecordRepository registry;
 private final CitizenRepository citizens;
 private final LookupAuditRepository audit;
 private final AuthSessionRepository sessions;
 ManagementController(ApiService auth, OfficerRepository officers, DefenseRecordRepository registry,
                      CitizenRepository citizens, LookupAuditRepository audit, AuthSessionRepository sessions) {
  this.auth=auth; this.officers=officers; this.registry=registry; this.citizens=citizens; this.audit=audit; this.sessions=sessions;
 }
 record OfficerRecord(String identification,String firstName,String lastName,String force,String rank,String forceId,String email,String role,boolean active) {}
 record Permission(@Pattern(regexp="ADMIN|OFFICER") @NotNull String role, boolean active) {}
 record RegistryInput(@Pattern(regexp="\\d{6,12}") @NotNull String identification,
  @NotBlank @Size(max=100) String force,@NotBlank @Size(max=100) String rank,
  @NotBlank @Size(max=100) String forceId,@Email @NotBlank @Size(max=254) String email,boolean active) {}
 record RegistryView(String identification,String force,String rank,String forceId,String email,boolean active) {}
 record AuditView(Long id,String officerId,String citizenId,String outcome,Instant occurredAt) {}
 record Summary(long citizens,long required,long officers,long lookups) {}
 private void admin(String token) { auth.requireAdmin(auth.require(token)); }
 @GetMapping("/admin/summary") Summary summary(@RequestHeader("Authorization") String token) {
  admin(token); return new Summary(citizens.count(), citizens.findAll().stream().filter(c->c.required).count(), officers.count(), audit.count());
 }
 @GetMapping("/admin/officers") List<OfficerRecord> officers(@RequestHeader("Authorization") String token) {
  admin(token); return officers.findAll(Sort.by("id")).stream().map(o->new OfficerRecord(o.id,o.firstName,o.lastName,o.forceName,o.rankName,o.forceId,o.email,o.role,o.active)).toList();
 }
 @Transactional
 @PatchMapping("/admin/officers/{id}") void permission(@RequestHeader("Authorization") String token,@PathVariable String id,@Valid @RequestBody Permission input) {
  Officer actor=auth.require(token); auth.requireAdmin(actor);
  if(actor.id.equals(id)) throw new ResponseStatusException(HttpStatus.CONFLICT,"No puedes cambiar tus propios permisos");
  Officer officer=officers.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Funcionario no encontrado"));
  officer.role=input.role();officer.active=input.active();officers.save(officer);sessions.deleteByOfficerId(id);
 }
 @GetMapping("/admin/registry") List<RegistryView> registry(@RequestHeader("Authorization") String token) {
  admin(token);return registry.findAll(Sort.by("documentId")).stream().map(d->new RegistryView(d.documentId,d.forceName,d.rankName,d.forceId,d.email,d.active)).toList();
 }
 @Transactional
 @PostMapping("/admin/registry") void registry(@RequestHeader("Authorization") String token,@Valid @RequestBody RegistryInput input) {
  admin(token);DefenseRecord d=registry.findById(input.identification()).orElseGet(DefenseRecord::new);
  d.documentId=input.identification();d.forceName=input.force();d.rankName=input.rank();d.forceId=input.forceId();d.email=input.email();d.active=input.active();registry.save(d);
 }
 @GetMapping("/history") Page<AuditView> history(@RequestHeader("Authorization") String token,
   @RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="30") @Min(1) @Max(100) int size) {
  Officer officer=auth.require(token);
  return audit.findByOfficerId(officer.id,PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"occurredAt").and(Sort.by(Sort.Direction.DESC,"id")))).map(ManagementController::view);
 }
 @GetMapping("/admin/audit") Page<AuditView> audit(@RequestHeader("Authorization") String token,
   @RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="30") @Min(1) @Max(100) int size) {
  admin(token);return audit.findAll(PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"occurredAt").and(Sort.by(Sort.Direction.DESC,"id")))).map(ManagementController::view);
 }
 private static AuditView view(LookupAudit a) { return new AuditView(a.id,a.officerId,a.citizenId,a.outcome,a.occurredAt); }
}
