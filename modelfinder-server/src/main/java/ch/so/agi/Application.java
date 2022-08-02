package ch.so.agi;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.filter.ForwardedHeaderFilter;
import org.springframework.nativex.hint.TypeHint;
import org.springframework.nativex.hint.TypeAccess;

import ch.so.agi.search.LuceneSearcher;

// Seems not to be necessary when building a musl image.
@TypeHint(
types = {org.springframework.http.converter.HttpMessageConverter[].class, 
        org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer[].class,
        org.springframework.boot.web.client.RestTemplateRequestCustomizer[].class,
        com.fasterxml.jackson.databind.JsonSerializer[].class,
        com.fasterxml.jackson.databind.JsonDeserializer[].class},
access= {TypeAccess.DECLARED_METHODS, 
    TypeAccess.DECLARED_FIELDS, 
    TypeAccess.DECLARED_CONSTRUCTORS, 
    TypeAccess.PUBLIC_METHODS,
    TypeAccess.PUBLIC_FIELDS,
    TypeAccess.PUBLIC_CONSTRUCTORS}               
)
@EnableScheduling
@SpringBootApplication
@ServletComponentScan
@Configuration
public class Application extends SpringBootServletInitializer {
    @Value("${app.connectTimeout}")
    private String connectTimeout;
    
    @Value("${app.readTimeout}")
    private String readTimeout;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
  
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(Application.class);
    }

    @Bean
    public ForwardedHeaderFilter forwardedHeaderFilter() {
        return new ForwardedHeaderFilter();
    }  
  
    // Anwendung ist fertig gestartet.
    @Bean
    public CommandLineRunner init(LuceneSearcher searcher) {
        return args -> {
            System.setProperty("sun.net.client.defaultConnectTimeout", connectTimeout);
            System.setProperty("sun.net.client.defaultReadTimeout", readTimeout);

            searcher.createIndex();
        };
    }
}
