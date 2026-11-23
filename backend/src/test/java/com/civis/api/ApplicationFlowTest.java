package com.civis.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import java.net.URI;
import java.net.http.*;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
 "spring.datasource.url=jdbc:h2:mem:civis;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
 "spring.datasource.driver-class-name=org.h2.Driver","spring.jpa.hibernate.ddl-auto=create-drop","civis.demo-mode=true"})
class ApplicationFlowTest {
 @LocalServerPort int port;
 @Autowired OfficerRepository officers;
 @org.springframework.test.context.bean.override.mockito.MockitoBean org.springframework.mail.javamail.JavaMailSender mail;
 final HttpClient client=HttpClient.newHttpClient();
 HttpResponse<String> call(String method,String path,String body,String token) throws Exception {
  var request=HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api"+path)).header("Content-Type","application/json");
  if(token!=null) request.header("Authorization","Bearer "+token);
  return client.send(request.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
 }
 String login(String id,String password) throws Exception {
  var response=call("POST","/auth/login","{\"identification\":\""+id+"\",\"password\":\""+password+"\"}",null);
  assertEquals(200,response.statusCode(),response.body());
  var matcher=Pattern.compile("\"token\":\"([^\"]+)\"").matcher(response.body());assertTrue(matcher.find());return matcher.group(1);
 }
 @Test void completeFlowAndAccessIsolation() throws Exception {
  var portal=client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/")).build(),HttpResponse.BodyHandlers.ofString());
  assertEquals(200,portal.statusCode());
  assertTrue(portal.body().contains("PORTAL ADMINISTRATIVO"));
  assertTrue(portal.headers().firstValue("Content-Security-Policy").orElse("").contains("frame-ancestors 'none'"));
  assertEquals(401,call("GET","/auth/me",null,"invalid").statusCode());
  String admin=login("1234567890","Demo"+"123");
  var authorization="{\"identification\":\"9000000077\",\"force\":\"Policía\",\"rank\":\"Patrullero\",\"forceId\":\"POL-TEST77\",\"email\":\"test77@civis.test\",\"active\":true}";
  assertEquals(200,call("POST","/admin/registry",authorization,admin).statusCode());
  var registration="{\"identification\":\"9000000077\",\"firstName\":\"Prueba\",\"lastName\":\"Registro\",\"force\":\"Policía\",\"rank\":\"Patrullero\",\"forceId\":\"POL-TEST77\",\"email\":\"test77@civis.test\"}";
  assertEquals(403,call("POST","/auth/register",registration.replace("POL-TEST77","INCORRECTO"),null).statusCode());
  assertEquals(201,call("POST","/auth/register",registration,null).statusCode());
  var sent=org.mockito.ArgumentCaptor.forClass(org.springframework.mail.SimpleMailMessage.class);
  org.mockito.Mockito.verify(mail).send(sent.capture());
  String temporary=sent.getValue().getText().split(": ")[1].split("\\.")[0];
  String registered=login("9000000077",temporary);
  assertEquals(403,call("GET","/admin/registry",null,registered).statusCode());
  assertEquals(200,call("POST","/auth/recover","{\"identification\":\"9000000077\"}",null).statusCode());
  assertEquals(401,call("GET","/auth/me",null,registered).statusCode());
  assertEquals(200,call("GET","/auth/me",null,admin).statusCode());
  var citizen="{\"identification\":\"9000000011\",\"firstName\":\"Ana\",\"lastName\":\"Prueba\",\"birthDate\":\"1995-01-15\",\"birthPlace\":\"Bogotá\",\"issueDate\":\"2013-06-17\",\"issuePlace\":\"Bogotá\",\"rh\":\"+\",\"bloodGroup\":\"O\",\"heightCm\":165,\"required\":true}";
  assertEquals(201,call("POST","/admin/citizens",citizen,admin).statusCode());
  assertEquals(409,call("POST","/admin/citizens",citizen,admin).statusCode());
  var lookup=call("GET","/citizens/9000000011/status",null,admin);
  assertEquals(200,lookup.statusCode());
  assertEquals("{\"status\":\"NO_REQUERIDO\"}",lookup.body());
  assertEquals(401,call("GET","/citizens/9000000011/status",null,null).statusCode());
  assertEquals(401,call("GET","/citizens/9000000011/status",null,"invalid").statusCode());
  assertEquals(200,call("PUT","/admin/citizens/9000000011",citizen,admin).statusCode());
  assertEquals(200,call("PUT","/admin/citizens/9000000011",citizen.replace("true","false"),admin).statusCode());
  assertEquals("{\"status\":\"NO_REQUERIDO\"}",call("GET","/citizens/9000000011/status",null,admin).body());
  assertEquals("{\"status\":\"SIN_DATOS\"}",call("GET","/citizens/999999999999/status",null,admin).body());
  assertTrue(call("GET","/history",null,admin).body().contains("9000000011"));
  assertEquals(200,call("GET","/admin/report.pdf",null,admin).statusCode());
  assertEquals(400,call("POST","/admin/citizens",citizen.replace("9000000011","invalid"),admin).statusCode());
  Officer o=new Officer();o.id="9000000099";o.firstName="Test";o.lastName="Officer";o.passwordHash=new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("Officer"+"12345");officers.save(o);
  String officer=login(o.id,"Officer"+"12345");
  assertEquals(403,call("GET","/admin/officers",null,officer).statusCode());
  assertFalse(call("GET","/history",null,officer).body().contains("9000000011"));
  assertEquals(403,call("POST","/admin/citizens",citizen,officer).statusCode());
  assertEquals(403,call("GET","/admin/status-history",null,officer).statusCode());
  long started=System.nanoTime();
  assertEquals("{\"status\":\"NO_REQUERIDO\"}",call("GET","/citizens/9000000011/status",null,officer).body());
  assertTrue(System.nanoTime()-started <= 5_000_000_000L,"Consulta tardó más de cinco segundos");
  assertTrue(call("GET","/history",null,officer).body().contains("9000000011"));
  var changes=call("GET","/admin/status-history",null,admin);
  assertEquals(200,changes.statusCode());
  assertTrue(changes.body().contains("9000000011"));
  assertTrue(changes.body().contains("\"previousRequired\":true"));
  assertTrue(changes.body().contains("\"newRequired\":false"));
  assertEquals(200,call("POST","/auth/change-password","{\"oldPassword\":\"Officer"+"12345\",\"newPassword\":\"Updated"+"12345\"}",officer).statusCode());
  assertEquals(401,call("GET","/auth/me",null,officer).statusCode());
  officer=login(o.id,"Updated"+"12345");
  assertEquals(200,call("PATCH","/admin/officers/"+o.id,"{\"role\":\"OFFICER\",\"active\":false}",admin).statusCode());
  assertEquals(401,call("GET","/auth/me",null,officer).statusCode());
  assertEquals(409,call("PATCH","/admin/officers/1234567890","{\"role\":\"OFFICER\",\"active\":false}",admin).statusCode());
  assertEquals(200,call("DELETE","/admin/citizens/9000000011",null,admin).statusCode());
  assertEquals(404,call("DELETE","/admin/citizens/9000000011",null,admin).statusCode());
  assertEquals(200,call("POST","/auth/logout","{}",admin).statusCode());
  assertEquals(401,call("GET","/auth/me",null,admin).statusCode());
 }
}
