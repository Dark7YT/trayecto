package com.trayecto.analytics.interfaces.rest.dto;

import java.util.List;
import java.util.UUID;

/**
 * Body para los endpoints {@code POST /reports/custom.{format}} y
 * {@code POST /reports/shared/{ownerId}/custom.{format}}.
 *
 * @param tripIds IDs de viajes a incluir. {@code null} o vacío → exporta todos
 *                los COMPLETED del target.
 */
public record CustomReportRequest(List<UUID> tripIds) {}
