package com.civis.api;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ApiServiceTest {
 @Test void sha256TokenDeterministicAndNotPlaintext(){
   assertEquals(ApiService.hash("same"),ApiService.hash("same"));
   assertNotEquals("same", ApiService.hash("same"));
   assertEquals(64,ApiService.hash("same").length());
 }
}
