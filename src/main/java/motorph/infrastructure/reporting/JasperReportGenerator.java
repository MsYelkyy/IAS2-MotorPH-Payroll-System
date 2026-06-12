package motorph.infrastructure.reporting;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.view.JasperViewer;

import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

/**
 * Handles JasperReports preview, print, and PDF export.
 */
public class JasperReportGenerator {

    // generates a filled Jasper report from a JRXML resource template
    public JasperPrint generateReport(String resourcePath,
                                      Map<String, Object> parameters,
                                      Collection<?> data) {
        try {
            InputStream reportStream = getClass().getResourceAsStream(resourcePath);

            if (reportStream == null) {
                throw new IllegalArgumentException("Report template not found: " + resourcePath);
            }

            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(data);

            return JasperFillManager.fillReport(
                    JasperCompileManager.compileReport(reportStream),
                    parameters,
                    dataSource
            );
        } catch (JRException e) {
            throw new RuntimeException("Failed to generate Jasper report.", e);
        }
    }

    // shows the report preview window
    public void showPreview(JasperPrint jasperPrint) {
        if (jasperPrint == null) {
            throw new IllegalArgumentException("Jasper print is required.");
        }

        JasperViewer.viewReport(jasperPrint, false);
    }

    // exports the report as a PDF file
    public void exportToPdf(JasperPrint jasperPrint, String outputPath) {
        if (jasperPrint == null) {
            throw new IllegalArgumentException("Jasper print is required.");
        }

        if (outputPath == null || outputPath.isBlank()) {
            throw new IllegalArgumentException("Output path is required.");
        }

        try {
            JasperExportManager.exportReportToPdfFile(jasperPrint, outputPath);
        } catch (JRException e) {
            throw new RuntimeException("Failed to export Jasper report to PDF.", e);
        }
    }
}