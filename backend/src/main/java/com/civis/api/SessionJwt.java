package com.civis.api;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
final class SessionJwt {
 private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
 private static final String HEADER = encode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
 private final SecureRandom random = new SecureRandom();
 private final SecretKeySpec key;

 SessionJwt(@Value("${civis.jwt-key:}") String configuredKey) {
  byte[] bytes;
  if (configuredKey.isBlank()) {
   bytes = new byte[32];
   random.nextBytes(bytes);
  } else {
   bytes = Base64.getDecoder().decode(configuredKey);
   if (bytes.length < 32) {
    throw new IllegalArgumentException("CIVIS_JWT_KEY must contain at least 32 bytes encoded in Base64");
   }
  }
  key = new SecretKeySpec(bytes, "HmacSHA256");
 }

 String issue(Instant issued, Instant expires) {
  byte[] nonce = new byte[24];
  random.nextBytes(nonce);
  String claims = "{\"iss\":\"CIVIS\",\"iat\":" + issued.getEpochSecond()
   + ",\"exp\":" + expires.getEpochSecond() + ",\"jti\":\"" + URL_ENCODER.encodeToString(nonce) + "\"}";
  String content = HEADER + "." + encode(claims);
  return content + "." + URL_ENCODER.encodeToString(sign(content));
 }

 boolean isSignedByCivis(String token) {
  if (token.length() > 1024 || !token.startsWith(HEADER + ".")) return false;
  int signature = token.indexOf('.', HEADER.length() + 1);
  if (signature <= HEADER.length() + 1 || token.length() - signature != 44) return false;

  String expected = URL_ENCODER.encodeToString(sign(token.substring(0, signature)));
  return MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),
   token.substring(signature + 1).getBytes(StandardCharsets.UTF_8));
 }

 private byte[] sign(String content) {
  try {
   Mac mac = Mac.getInstance("HmacSHA256");
   mac.init(key);
   return mac.doFinal(content.getBytes(StandardCharsets.US_ASCII));
  } catch (GeneralSecurityException e) {
   throw new IllegalStateException("Could not sign CIVIS session token", e);
  }
 }

 private static String encode(String text) {
  return URL_ENCODER.encodeToString(text.getBytes(StandardCharsets.UTF_8));
 }
}
