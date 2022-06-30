package ch.so.agi;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ForwardedHeaderFilter;

import ch.so.agi.search.LuceneSearcher;

@SpringBootApplication
@ServletComponentScan
@Configuration
public class Application extends SpringBootServletInitializer {
  
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
            searcher.createIndex();
        };
    }
}
