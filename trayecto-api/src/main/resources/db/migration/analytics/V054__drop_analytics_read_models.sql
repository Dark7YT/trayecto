-- Analytics ya no usa read-models materializados: las 4 queries (dashboard,
-- km-by-month, cost-by-month, multiplier-usage) agregan EN VIVO leyendo los
-- viajes COMPLETED reales vía TripsPublicApi. Esto elimina la posibilidad de
-- que las estadísticas se desincronicen (al añadir el módulo después de tener
-- datos, al borrar viajes COMPLETED, o al fallar el proyector silenciosamente).
--
-- Borramos las 4 tablas. Las migrations V050..V053 quedan en el historial.

DROP TABLE IF EXISTS analytics_multiplier_usage;
DROP TABLE IF EXISTS analytics_cost_by_month;
DROP TABLE IF EXISTS analytics_trips_by_month;
DROP TABLE IF EXISTS analytics_user_stats;
