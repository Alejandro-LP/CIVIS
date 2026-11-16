package com.civis.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.transaction.annotation.Transactional;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.nio.charset.StandardCharsets;

@Service
class ApiService {
 private final OfficerRepository officers;
 private final DefenseRecordRepository registry;
 private final AuthSessionRepository sessions;
 private final JavaMailSender mail;
 private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
 private final SecureRandom secure = new SecureRandom();
 @Value("${civis.mail-from}") String from;
 @Value("${civis.token-hours:8}") long tokenHours;
 @Value("${civis.demo-mode:false}") boolean demo;
 ApiService(OfficerRepository officers,DefenseRecordRepository registry,AuthSessionRepository sessions,JavaMailSender mail){this.officers=officers;this.registry=registry;this.sessions=sessions;this.mail=mail;}
 static String hash(String s){try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 String newPassword(){
   String up="ABCDEFGHJKLMNPQRSTUVWXYZ", low="abcdefghjkmnpqrstuvwxyz", digits="23456789", all=up+low+digits;
   char[] chars={up.charAt(secure.nextInt(up.length())),low.charAt(secure.nextInt(low.length())),digits.charAt(secure.nextInt(digits.length())),all.charAt(secure.nextInt(all.length())),all.charAt(secure.nextInt(all.length())),all.charAt(secure.nextInt(all.length()))};
   for(int i=chars.length-1;i>0;i--){int j=secure.nextInt(i+1);char t=chars[i];chars[i]=chars[j];chars[j]=t;}
   return new String(chars);
 }
 @Transactional
 void register(ApiController.Register r){
   if(officers.existsById(r.identification()) || officers.existsByEmailIgnoreCase(r.email())) throw new ResponseStatusException(HttpStatus.CONFLICT,"Funcionario ya registrado");
   DefenseRecord d=registry.findById(r.identification()).orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN,"No consta autorización en el registro institucional"));
   if(!d.active || !d.forceName.equals(r.force()) || !d.rankName.equals(r.rank()) || !d.forceId.equals(r.forceId()) || !d.email.equalsIgnoreCase(r.email())) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Datos institucionales no coinciden");
   String password=newPassword();
   // Primero se intenta entregar el correo. Si SMTP falla, la transacción no registra la cuenta.
   SimpleMailMessage message=new SimpleMailMessage();message.setFrom(from);message.setTo(r.email());message.setSubject("CIVIS - contraseña temporal");message.setText("Contraseña temporal de CIVIS: "+password+". Cambia tu contraseña al iniciar sesión.");
   try{mail.send(message);}catch(Exception ex){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Servicio de correo no disponible");}
   Officer o=new Officer();o.id=r.identification();o.firstName=r.firstName();o.lastName=r.lastName();o.forceName=r.force();o.rankName=r.rank();o.forceId=r.forceId();o.email=r.email();o.passwordHash=encoder.encode(password);officers.save(o);
 }
 @Transactional
 ApiController.LoginResponse login(ApiController.Login r){
   Officer o=officers.findById(r.identification()).orElse(null);
   if(o==null || !o.active || !encoder.matches(r.password(),o.passwordHash))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Credenciales inválidas");
   byte[] b=new byte[32];secure.nextBytes(b);String token=Base64.getUrlEncoder().withoutPadding().encodeToString(b);
   AuthSession s=new AuthSession();s.tokenHash=hash(token);s.officer=o;s.expiresAt=Instant.now().plus(tokenHours,ChronoUnit.HOURS);sessions.save(s);
   return new ApiController.LoginResponse(token,new ApiController.OfficerView(o.id,o.firstName,o.lastName,o.forceName,o.rankName,o.forceId,o.email,o.role),demo);
 }
 Officer require(String authorization){
   if(authorization==null || !authorization.startsWith("Bearer "))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Inicia sesión");
   AuthSession s=sessions.findById(hash(authorization.substring(7))).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Sesión no válida"));
   if(s.expiresAt.isBefore(Instant.now()) || !s.officer.active)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Sesión expirada");
   return s.officer;
 }
 void requireAdmin(Officer o){if(!"ADMIN".equals(o.role))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Permiso insuficiente");}
 @Transactional void logout(String auth){if(auth!=null && auth.startsWith("Bearer "))sessions.deleteById(hash(auth.substring(7)));}
 @Transactional void recover(String identification){
   Officer o=officers.findById(identification).orElse(null);if(o==null || !o.active) return;
   String password=newPassword();SimpleMailMessage message=new SimpleMailMessage();message.setFrom(from);message.setTo(o.email);message.setSubject("CIVIS - recuperación");message.setText("Nueva contraseña temporal CIVIS: "+password);
   try{mail.send(message);}catch(Exception ex){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Correo no disponible");}
   o.passwordHash=encoder.encode(password);sessions.deleteByOfficerId(o.id);
 }
 @Transactional void changePassword(Officer o,String oldPass,String newPass){
  if(!encoder.matches(oldPass,o.passwordHash))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Contraseña actual incorrecta");
  if(newPass==null || newPass.length()<10 || newPass.getBytes(StandardCharsets.UTF_8).length>72 || !newPass.matches("(?s).*[A-Z].*") || !newPass.matches("(?s).*[a-z].*") || !newPass.matches("(?s).*\\d.*"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"La contraseña nueva debe tener al menos 10 caracteres, máximo 72 bytes y mayúscula, minúscula y número");
  o.passwordHash=encoder.encode(newPass);
  officers.save(o);
  sessions.deleteByOfficerId(o.id);
 }
}
