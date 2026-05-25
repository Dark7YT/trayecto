package com.trayecto.analytics.infrastructure.reports;

import com.trayecto.analytics.application.ReportFormatPort;
import org.springframework.stereotype.Component;

/**
 * Fachada del port {@link ReportFormatPort} que delega a los dos adapters concretos:
 * OpenPDF para PDF y Apache POI para Excel.
 */
@Component
class ReportFormatAdapter implements ReportFormatPort {

    private final OpenPdfReportAdapter pdfAdapter = new OpenPdfReportAdapter();
    private final ApachePoiReportAdapter xlsxAdapter = new ApachePoiReportAdapter();

    @Override
    public byte[] generateReportPdf(ReportData data) {
        return pdfAdapter.generate(data);
    }

    @Override
    public byte[] generateReportXlsx(ReportData data) {
        return xlsxAdapter.generate(data);
    }
}
