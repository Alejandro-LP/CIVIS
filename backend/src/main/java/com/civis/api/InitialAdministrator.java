package com.civis.api;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
class InitialAdministrator {
 @Bean CommandLineRunner initializeAdministrator(OfficerRepository officers,
  @Value("${civis.demo-mode:false}") boolean demo,
  @Value("${CIVIS_ADMIN_ID:}") String id,@Value("${CIVIS_ADMIN_PASSWORD:}") String password,
  @Value("${CIVIS_ADMIN_EMAIL:}") String email) {
  return args -> {
   if(demo || officers.count()>0) return;
   if(!id.matches("\\d{6,12}") || password.length()<12 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72 ||
      !password.matches("(?s).*[A-Z].*") || !password.matches("(?s).*[a-z].*") || !password.matches("(?s).*\\d.*") ||
      !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
    throw new IllegalStateException("Base vacía: configura CIVIS_ADMIN_ID (6-12 dígitos), CIVIS_ADMIN_PASSWORD (12-72 bytes, mayúscula, minúscula y número) y CIVIS_ADMIN_EMAIL.");
   Officer o=new Officer();o.id=id;o.firstName="Administrador";o.lastName="CIVIS";o.forceName="Administración";o.rankName="Administrador";
   o.forceId="ADMIN-"+id;o.email=email;o.role="ADMIN";o.passwordHash=new BCryptPasswordEncoder(12).encode(password);officers.save(o);
  };
 }
}
