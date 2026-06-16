; package com.todo.app.entity;
;
; import jakarta.persistence.*;
; import jakarta.persistence.Entity;
; import org.springframework.format.annotation.DateTimeFormat;
;
; import java.time.LocalDate;
;
; @Entity
; @Table(name = "tasks")
; public class Task {

; Search about jakarta.persistence.* or jakarta.persistence.Enrity
(import_declaration
  (scoped_identifier) @import.name
  (#eq? @import.name "jakarta.persistence.Entity")
)

; Search about Entity annotation => @Entity
(marker_annotation
  name: (identifier) @annotation.name
  (#eq? @annotation.name "Entity")
)