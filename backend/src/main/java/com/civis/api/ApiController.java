package com.civis.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Value;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@RestController @RequestMapping("/api")
public class ApiController {
 private final ApiService auth;
 private final CitizenRepository citizens;
 private final LookupAuditRepository audit;
 @Value("${civis.demo-mode:false}") boolean demo;
 ApiController(ApiService auth,CitizenRepository citizens,LookupAuditRepository audit){this.auth=auth;this.citizens=citizens;this.audit=audit;}
 public record Register(@NotBlank String identification,@NotBlank String firstName,@NotBlank String lastName,@NotBlank String force,@NotBlank String rank,@NotBlank String forceId,@Email @NotBlank String email) {}
 public record Login(@NotBlank String identification,@NotBlank String password) {}
 public record Recovery(@NotBlank String identification) {}
 public record PasswordChange(@NotBlank String oldPassword,@NotBlank String newPassword) {}
 public record OfficerView(String identification,String firstName,String lastName,String force,String rank,String forceId,String email,String role) {}
 public record LoginResponse(String token,OfficerView officer,boolean demo) {}
 public record LookupResult(String identification,String status,String name,boolean demo,String message,Instant checkedAt) {}
 public record CitizenInput(@NotBlank @Pattern(regexp="\\d{6,12}") String identification,@NotBlank @Size(max=100) String firstName,@NotBlank @Size(max=100) String lastName,@PastOrPresent LocalDate birthDate,@Size(max=150) String birthPlace,@PastOrPresent LocalDate issueDate,@Size(max=150) String issuePlace,@Pattern(regexp="[+-]") String rh,@Pattern(regexp="A|B|AB|O") String bloodGroup,@Min(40) @Max(250) Integer heightCm,boolean required) {}
 @GetMapping("/health") public Map<String,Object> health(){return Map.of("status","UP","demo",demo);}
 @PostMapping("/auth/register") public ResponseEntity<Map<String,String>> register(@Valid @RequestBody Register request){auth.register(request);return ResponseEntity.status(201).body(Map.of("message","Registro correcto. Revisa tu correo para obtener la contraseña temporal."));}
 @PostMapping("/auth/login") public LoginResponse login(@Valid @RequestBody Login request){return auth.login(request);}
 @PostMapping("/auth/recover") public Map<String,String> recover(@Valid @RequestBody Recovery request){auth.recover(request.identification());return Map.of("message","Si la identificación existe, se enviarán instrucciones al correo registrado.");}
 @PostMapping("/auth/logout") public void logout(@RequestHeader(value="Authorization",required=false) String token){auth.logout(token);}
 @PostMapping("/auth/change-password") public Map<String,String> change(@RequestHeader("Authorization") String token,@Valid @RequestBody PasswordChange p){auth.changePassword(auth.require(token),p.oldPassword(),p.newPassword());return Map.of("message","Contraseña actualizada");}
 @GetMapping("/auth/me") public OfficerView me(@RequestHeader("Authorization") String token){Officer o=auth.require(token);return new OfficerView(o.id,o.firstName,o.lastName,o.forceName,o.rankName,o.forceId,o.email,o.role);}
 @GetMapping("/citizens/{id}/status") public LookupResult status(@RequestHeader("Authorization") String token,@PathVariable String id){
  Officer officer=auth.require(token);
  if(!id.matches("\\d{6,12}"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Identificación inválida");
  Optional<Citizen> c=citizens.findById(id);String outcome=c.map(x->x.required?"REQUERIDO":"NO_REQUERIDO").orElse("SIN_DATOS");
  LookupAudit a=new LookupAudit();a.officerId=officer.id;a.citizenId=id;a.outcome=outcome;a.occurredAt=Instant.now();audit.save(a);
  return new LookupResult(id,outcome,c.map(x->x.firstName+" "+x.lastName).orElse(""),demo,demo?"Resultado en entorno de demostración, no constituye antecedente judicial oficial.":"Resultado del registro interno CIVIS. No acredita antecedentes de una fuente oficial.",Instant.now());
 }
 // El portal web y Android comparten los mismos controles de permisos en la API.
 @PostMapping("/admin/citizens") public ResponseEntity<Map<String,String>> save(@RequestHeader("Authorization") String token,@Valid @RequestBody CitizenInput in){
  auth.requireAdmin(auth.require(token));
  if(in.birthDate()!=null && in.issueDate()!=null && in.issueDate().isBefore(in.birthDate()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"La expedición no puede ser anterior al nacimiento");
  Citizen c=citizens.findById(in.identification()).orElseGet(Citizen::new);
  c.documentId=in.identification();c.firstName=in.firstName();c.lastName=in.lastName();c.birthDate=in.birthDate();c.birthPlace=in.birthPlace();c.issueDate=in.issueDate();c.issuePlace=in.issuePlace();c.rh=in.rh();c.bloodGroup=in.bloodGroup();c.heightCm=in.heightCm();c.required=in.required();citizens.save(c);
  return ResponseEntity.ok(Map.of("message","Ciudadano guardado"));
 }
 @DeleteMapping("/admin/citizens/{id}") public Map<String,String> delete(@RequestHeader("Authorization") String token,@PathVariable String id){
  auth.requireAdmin(auth.require(token));if(!citizens.existsById(id))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Ciudadano no encontrado");citizens.deleteById(id);return Map.of("message","Ciudadano eliminado");
 }
 @GetMapping("/admin/citizens") public List<Map<String,Object>> list(@RequestHeader("Authorization") String token,@RequestParam(required=false) Boolean required){
  auth.requireAdmin(auth.require(token));return citizens.findAll(org.springframework.data.domain.Sort.by("documentId")).stream().filter(c->required==null || c.required==required).map(c->{Map<String,Object> m=new LinkedHashMap<>();m.put("identification",c.documentId);m.put("firstName",c.firstName);m.put("lastName",c.lastName);m.put("required",c.required);m.put("birthDate",c.birthDate);m.put("birthPlace",c.birthPlace);m.put("issueDate",c.issueDate);m.put("issuePlace",c.issuePlace);m.put("rh",c.rh);m.put("bloodGroup",c.bloodGroup);m.put("heightCm",c.heightCm);return m;}).toList();
 }
}
