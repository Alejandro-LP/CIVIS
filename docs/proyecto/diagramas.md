# Diagramas de CIVIS

Los diagramas describen la implementación académica. El padrón institucional es un componente simulado dentro de la base del proyecto.

## Contexto y despliegue

```mermaid
flowchart LR
    A[Administrador] --> W[Portal web]
    F[Funcionario] --> M[Android Kotlin]
    W -->|REST JSON y Bearer| S[Spring MVC y Tomcat]
    M -->|REST JSON y Bearer| S
    S --> O[Hibernate JPA]
    O --> D[(PostgreSQL)]
    S --> C[SMTP]
    C --> B[Correo del funcionario]
    D --- P[Padrón académico de autorizaciones]
```

## Modelo entidad relación

```mermaid
erDiagram
    OFFICERS ||--o{ AUTH_SESSIONS : tiene
    OFFICERS ||--o{ LOOKUP_AUDIT : registra
    CITIZENS ||--o{ LOOKUP_AUDIT : consulta_por_documento
    DEFENSE_REGISTRY ||--o| OFFICERS : autoriza_registro
    OFFICERS {
        varchar id PK
        varchar password_hash
        varchar role
        boolean active
    }
    AUTH_SESSIONS {
        varchar token_hash PK
        varchar officer_id FK
        timestamp expires_at
    }
    CITIZENS {
        varchar document_id PK
        varchar first_name
        varchar last_name
        date birth_date
        date issue_date
        boolean required
    }
    DEFENSE_REGISTRY {
        varchar document_id PK
        varchar force_id UK
        varchar force_name
        varchar rank_name
        varchar email
        boolean active
    }
    LOOKUP_AUDIT {
        bigint id PK
        varchar officer_id
        varchar citizen_id
        varchar outcome
        timestamp occurred_at
    }
```

La relación de auditoría por documento es lógica. La tabla conserva el documento consultado aun cuando el ciudadano no existe o su ficha se elimina. La asociación entre padrón y funcionario también es lógica. La clave foránea física de sesión sí referencia officers.

## Registro y autenticación

```mermaid
sequenceDiagram
    actor F as Funcionario
    participant M as Android
    participant A as API
    participant P as Padrón académico
    participant C as SMTP
    participant D as PostgreSQL
    F->>M: Completa los siete datos
    M->>A: POST auth/register
    A->>P: Comprueba autorización y coincidencia
    P-->>A: Autorizado
    A->>A: Genera clave de seis caracteres
    A->>C: Entrega contraseña temporal
    A->>D: Guarda funcionario y hash BCrypt
    A-->>M: Registro exitoso
    F->>M: Documento y clave recibida
    M->>A: POST auth/login
    A->>D: Verifica hash y estado activo
    A->>D: Guarda hash del token y expiración
    A-->>M: Token y perfil
```

## Consulta y auditoría

```mermaid
sequenceDiagram
    actor F as Funcionario
    participant M as Android
    participant A as API
    participant D as PostgreSQL
    F->>M: Lee código o introduce documento
    M->>F: Solicita confirmar identificación
    F->>M: Confirma
    M->>A: GET citizens/id/status con Bearer
    A->>D: Valida sesión y busca documento
    D-->>A: Estado registrado o ausencia
    A->>D: Guarda auditoría con usuario y fecha
    A-->>M: REQUERIDO o NO_REQUERIDO o SIN_DATOS
    M-->>F: Nombre y resultado
```
