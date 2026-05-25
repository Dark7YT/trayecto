-- ============================================================================
-- Baseline migration for the Trayecto database.
--
-- Versioning scheme across modules (avoids Flyway collisions):
--   001-009  -> common (extensions, shared utilities)
--   010-019  -> iam
--   020-029  -> trips
--   030-039  -> sharing
--   040-049  -> notifications
--   050-059  -> analytics
--
-- Cross-module foreign keys are NOT created. Each module owns its tables.
-- The user_id column in trips/sharing/etc. is a logical reference; enforced
-- at the application layer through events and the iam.api public interface.
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";
