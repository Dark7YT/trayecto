-- ============================================================================
-- Spring Modulith 2.0.x amplió el schema de event_publication con tres columnas
-- adicionales para soportar el flujo de reintentos y estados:
--   - completion_attempts: cuántas veces se intentó procesar el evento
--   - last_resubmission_date: timestamp del último reintento
--   - status: PUBLISHED | RESUBMITTED | COMPLETED (estado actual del outbox row)
--
-- Forward-only migration: dejamos V002 como creación inicial (compatible con
-- versiones antiguas de Modulith) y aquí ampliamos hasta el schema 2.0.x.
-- ============================================================================

ALTER TABLE event_publication
    ADD COLUMN completion_attempts    INTEGER     NOT NULL DEFAULT 0,
    ADD COLUMN last_resubmission_date TIMESTAMP(6) WITH TIME ZONE,
    ADD COLUMN status                 VARCHAR(16) NOT NULL DEFAULT 'PUBLISHED';

ALTER TABLE event_publication_archive
    ADD COLUMN completion_attempts    INTEGER     NOT NULL DEFAULT 0,
    ADD COLUMN last_resubmission_date TIMESTAMP(6) WITH TIME ZONE,
    ADD COLUMN status                 VARCHAR(16) NOT NULL DEFAULT 'PUBLISHED';

CREATE INDEX event_publication_by_status_idx
    ON event_publication (status)
    WHERE completion_date IS NULL;
