-- Consultas útiles para demostrar la persistencia; las tablas las crea JPA en DEMO_MODE.
SELECT id, officer_id, citizen_id, outcome, occurred_at FROM lookup_audit ORDER BY id DESC;
SELECT document_id, first_name, last_name, required FROM citizens ORDER BY document_id;
SELECT id, first_name, last_name, force_name, rank_name, role FROM officers;
