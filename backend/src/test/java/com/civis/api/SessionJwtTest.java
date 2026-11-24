package com.civis.api;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class SessionJwtTest {
 @Test void issuesUniqueSignedTokensWithExpiry() {
  SessionJwt jwt = new SessionJwt("");
  Instant issued = Instant.parse("2026-01-01T00:00:00Z");
  Instant expires = issued.plusSeconds(600);
  String token = jwt.issue(issued, expires);
  assertTrue(jwt.isSignedByCivis(token));
  assertNotEquals(token, jwt.issue(issued, expires));
  String[] parts = token.split("\\.");
  assertEquals(3, parts.length);
  String claims = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
  assertTrue(claims.contains("\"exp\":" + expires.getEpochSecond()));
  assertFalse(jwt.isSignedByCivis(token + "A"));
  assertFalse(new SessionJwt("").isSignedByCivis(token));
 }

 @Test void configuredKeySurvivesRestart() {
  String key = Base64.getEncoder().encodeToString(new byte[32]);
  Instant now = Instant.now();
  String token = new SessionJwt(key).issue(now, now.plusSeconds(600));
  assertTrue(new SessionJwt(key).isSignedByCivis(token));
  assertThrows(IllegalArgumentException.class, () -> new SessionJwt("YQ=="));
 }
}
