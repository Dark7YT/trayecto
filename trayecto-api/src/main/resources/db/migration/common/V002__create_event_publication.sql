-- ============================================================================
-- Tablas requeridas por Spring Modulith Events (JPA backend).
-- Modulith persiste cada evento de dominio aquí dentro de la misma transacción
-- que lo emite (patrón Outbox) y los listeners @ApplicationModuleListener marcan
-- completion_date al consumir. Si la app crashea entre commit y handler, al
-- reiniciar Modulith reintenta los publications sin completion_date.
--
-- Las archived tables almacenan publications completados para auditoría.
-- ============================================================================

CREATE TABLE event_publication (
    id                UUID                     NOT NULL,
    listener_id       VARCHAR(512)             NOT NULL,
    event_type        VARCHAR(512)             NOT NULL,
    serialized_event  TEXT                     NOT NULL,
    publication_date  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    completion_date   TIMESTAMP(6) WITH TIME ZONE,
    PRIMARY KEY (id)
);

CREATE INDEX event_publication_serialized_event_hash_idx
    ON event_publication (serialized_event);

CREATE INDEX event_publication_by_completion_date_idx
    ON event_publication (completion_date);


CREATE TABLE event_publication_archive (
    id                UUID                     NOT NULL,
    listener_id       VARCHAR(512)             NOT NULL,
    event_type        VARCHAR(512)             NOT NULL,
    serialized_event  TEXT                     NOT NULL,
    publication_date  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    completion_date   TIMESTAMP(6) WITH TIME ZONE,
    PRIMARY KEY (id)
);

CREATE INDEX event_publication_archive_by_completion_date_idx
    ON event_publication_archive (completion_date);
