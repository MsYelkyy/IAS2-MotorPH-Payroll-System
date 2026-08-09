package motorph.backend.reporting;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

/**
 * Server-side Jasper report generation. Unlike the desktop's original
 * JasperReportGenerator, this has no showPreview() (Swing, not available
 * headless) and no file-based exportToPdf() (a backend has no meaningful
 * local output path) - callers get PDF bytes to stream in an HTTP response.
 */
@Component
public class JasperReportGenerator {

    public JasperPrint generateReport(String resourcePath, Map<String, Object> parameters, Collection<?> data) {
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

    public byte[] exportToPdfBytes(JasperPrint jasperPrint) {
        if (jasperPrint == null) {
            throw new IllegalArgumentException("Jasper print is required.");
        }

        try {
            return JasperExportManager.exportReportToPdf(jasperPrint);
        } catch (JRException e) {
            throw new RuntimeException("Failed to export Jasper report to PDF.", e);
        }
    }
}
