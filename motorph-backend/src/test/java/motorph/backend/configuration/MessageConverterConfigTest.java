package motorph.backend.configuration;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MessageConverterConfigTest {

    private final MessageConverterConfig config = new MessageConverterConfig();

    @Test
    @DisplayName("Only the Jackson XML converter is removed; the others keep their place")
    void removesOnlyTheXmlConverter() {
        List<HttpMessageConverter<?>> converters = new ArrayList<>(List.of(
                new ByteArrayHttpMessageConverter(),
                new StringHttpMessageConverter(),
                new MappingJackson2HttpMessageConverter(),
                new MappingJackson2XmlHttpMessageConverter()));

        config.extendMessageConverters(converters);

        assertThat(converters).hasExactlyElementsOfTypes(
                ByteArrayHttpMessageConverter.class,
                StringHttpMessageConverter.class,
                MappingJackson2HttpMessageConverter.class);
    }

    @Test
    @DisplayName("A list without an XML converter is left as it was")
    void leavesAListWithoutXmlAlone() {
        List<HttpMessageConverter<?>> converters = new ArrayList<>(List.of(
                new StringHttpMessageConverter(), new MappingJackson2HttpMessageConverter()));

        config.extendMessageConverters(converters);

        assertThat(converters).hasSize(2);
    }

    @Test
    @DisplayName("The XML library itself is untouched, because JasperReports reads its templates with it")
    void xmlLibraryRemainsAvailable() throws Exception {
        XmlMapper xmlMapper = new XmlMapper();

        assertThat(xmlMapper.readTree("<a><b>1</b></a>").get("b").asInt()).isEqualTo(1);
    }
}
