package com.civis.api;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
class DemoData {
 @Bean CommandLineRunner seed(OfficerRepository officers,DefenseRecordRepository registry,CitizenRepository citizens,@Value("${civis.demo-mode:true}") boolean demo){return args->{
  if(!demo)return;
  if(!officers.existsById("1234567890")){
   Officer o=new Officer();o.id="1234567890";o.firstName="Andrea";o.lastName="Morales";o.forceName="Policía";o.rankName="Patrullero";o.forceId="POL-001";o.email="demo@civis.test";o.passwordHash=new BCryptPasswordEncoder(12).encode("Demo123");o.role="ADMIN";officers.save(o);
  }
  if(!registry.existsById("1111111111")){
   DefenseRecord d=new DefenseRecord();d.documentId="1111111111";d.forceName="Policía";d.rankName="Patrullero";d.forceId="POL-101";d.email="funcionario@civis.test";registry.save(d);
  }
  // Mantener compatibilidad con los dos identificadores usados en la primera demo.
  for(var entry:java.util.Map.of("1000000001",false,"1000000002",true).entrySet()){
   if(!citizens.existsById(entry.getKey())){Citizen c=new Citizen();c.documentId=entry.getKey();c.firstName="Ciudadano";c.lastName="De prueba";c.required=entry.getValue();citizens.save(c);}
  }
  // Diez ciudadanos enteramente ficticios, persistidos en PostgreSQL una sola vez.
  // No sobrescribir cambios del usuario al reiniciar el backend.
  addCitizen(citizens,"1000000011", "Laura", "Cárdenas", "1993-02-14", "Bogotá", "2011-03-02", "Bogotá", "+", "O", 164, false);
  addCitizen(citizens,"1000000012", "Mateo", "Salazar", "1988-07-21", "Medellín", "2006-09-10", "Medellín", "+", "A", 176, true);
  addCitizen(citizens,"1000000013", "Valentina", "Rojas", "1999-11-05", "Cali", "2017-12-01", "Cali", "-", "B", 159, false);
  addCitizen(citizens,"1000000014", "Sebastián", "Pineda", "1985-04-17", "Barranquilla", "2003-05-22", "Barranquilla", "+", "AB", 181, true);
  addCitizen(citizens,"1000000015", "Camila", "Torres", "1996-09-03", "Bucaramanga", "2014-10-15", "Bucaramanga", "+", "B", 167, false);
  addCitizen(citizens,"1000000016", "Nicolás", "Herrera", "1991-12-29", "Pereira", "2010-01-13", "Pereira", "-", "O", 174, false);
  addCitizen(citizens,"1000000017", "Daniela", "Acosta", "2000-06-09", "Manizales", "2018-07-19", "Manizales", "+", "A", 162, true);
  addCitizen(citizens,"1000000018", "Samuel", "Vargas", "1987-03-26", "Cartagena", "2005-04-28", "Cartagena", "+", "O", 179, false);
  addCitizen(citizens,"1000000019", "Isabella", "Mendoza", "1995-08-12", "Tunja", "2013-09-04", "Tunja", "+", "AB", 165, false);
  addCitizen(citizens,"1000000020", "Andrés", "Quintero", "1990-01-30", "Ibagué", "2008-02-25", "Ibagué", "-", "A", 172, true);
 };}
 private static void addCitizen(CitizenRepository repo, String id, String first, String last,
     String born, String birthplace, String issued, String issueplace, String rh,
     String group, int height, boolean required) {
  if(repo.existsById(id)) return;
  Citizen c = new Citizen();
  c.documentId = id;
  c.firstName = first;
  c.lastName = last;
  c.birthDate = java.time.LocalDate.parse(born);
  c.birthPlace = birthplace;
  c.issueDate = java.time.LocalDate.parse(issued);
  c.issuePlace = issueplace;
  c.rh = rh;
  c.bloodGroup = group;
  c.heightCm = height;
  c.required = required;
  repo.save(c);
 }
}
