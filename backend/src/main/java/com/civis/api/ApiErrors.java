package com.civis.api;

import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.Map;

@RestControllerAdvice
class ApiErrors {
 @ExceptionHandler(ResponseStatusException.class)
 ResponseEntity<Map<String,String>> status(ResponseStatusException ex) {
  return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message",ex.getReason()==null?"Solicitud no válida":ex.getReason()));
 }
 @ExceptionHandler(MethodArgumentNotValidException.class)
 ResponseEntity<Map<String,String>> validation(MethodArgumentNotValidException ex) {
  return ResponseEntity.badRequest().body(Map.of("message","Verifica los campos: "+ex.getBindingResult().getFieldErrors().stream().map(e->e.getField()).distinct().sorted().reduce((a,b)->a+", "+b).orElse("datos")));
 }
 @ExceptionHandler(DataIntegrityViolationException.class)
 ResponseEntity<Map<String,String>> conflict() {
  return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message","Los datos entran en conflicto con un registro existente"));
 }
}
