package com.trayecto.analytics.application;

import com.trayecto.analytics.application.ReportFormatPort.ReportData;
import com.trayecto.iam.api.IamPublicApi;
import com.trayecto.shared.kernel.UserId;
import com.trayecto.trips.api.TripsPublicApi;
import com.trayecto.trips.api.dto.TripSnapshot;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Reporte personalizado: el usuario selecciona los tripIds específicos que
 * quiere exportar (o "todos" si la selección llega vacía / null). A diferencia
 * de {@link GenerateMonthlyReportHandler}, no filtra por mes — abarca cualquier
 * conjunto arbitrario de viajes COMPLETED del usuario.
 *
 * Filtro de seguridad: cargamos los trips COMPLETED del userId target y luego
 * intersectamos con la lista solicitada. Así nunca se puede colar un tripId
 * de otro usuario aunque el cliente lo mande en el payload.
 */
@Component
public class GenerateCustomReportHandler {

    private final IamPublicApi iamApi;
    private final TripsPublicApi tripsApi;
    private final ReportFormatPort reportFormat;

    public GenerateCustomReportHandler(
        IamPublicApi iamApi,
        TripsPublicApi tripsApi,
        ReportFormatPort reportFormat
    ) {
        this.iamApi = iamApi;
        this.tripsApi = tripsApi;
        this.reportFormat = reportFormat;
    }

    /**
     * @param tripIds  IDs de viajes a incluir. Si es null o vacío → exporta TODOS
     *                 los COMPLETED del usuario.
     */
    public record Command(
        UserId userId,
        Set<UUID> tripIds,
        GenerateMonthlyReportHandler.Format format
    ) {}

    @Transactional(readOnly = true)
    public byte[] handle(Command cmd) {
        String displayName = iamApi.findUserSnapshot(cmd.userId())
            .map(s -> s.displayName())
            .orElse("Usuario");

        // Todos los COMPLETED del usuario (sin filtro de fecha — la selección
        // puede abarcar cualquier mes / año).
        List<TripSnapshot> all = tripsApi.listCompletedByUser(cmd.userId(), null, null);

        // Si llega selección, intersectamos; si no, exportamos todos.
        boolean selectAll = cmd.tripIds() == null || cmd.tripIds().isEmpty();
        List<TripSnapshot> filtered = selectAll
            ? all
            : all.stream()
                .filter(t -> cmd.tripIds().contains(t.tripId().value()))
                .toList();

        String periodLabel = selectAll
            ? String.format("Todos los viajes (%d)", filtered.size())
            : String.format("Selección personalizada (%d viajes)", filtered.size());
        String sheetName = selectAll ? "todos" : "seleccion";

        ReportData data = GenerateMonthlyReportHandler.buildReportData(
            displayName, periodLabel, sheetName, filtered
        );
        return switch (cmd.format()) {
            case PDF -> reportFormat.generateReportPdf(data);
            case XLSX -> reportFormat.generateReportXlsx(data);
        };
    }
}
