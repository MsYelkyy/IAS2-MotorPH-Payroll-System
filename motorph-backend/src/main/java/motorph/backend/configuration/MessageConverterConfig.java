package motorph.backend.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Makes the API JSON only. JasperReports brings in jackson-dataformat-xml, so Spring Boot also adds
 * an XML converter and every endpoint ends up accepting XML. This removes only that converter.
 * The XML library stays because JasperReports needs it to read report templates.
 */
@Configuration
public class MessageConverterConfig implements WebMvcConfigurer {

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.removeIf(MappingJackson2XmlHttpMessageConverter.class::isInstance);
    }
}
