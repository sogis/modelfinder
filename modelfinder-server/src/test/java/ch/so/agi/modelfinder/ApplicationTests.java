package ch.so.agi.modelfinder;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ApplicationTests {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;

	@Test
	void contextLoads() {
	}

    @Test
    public void index_Ok() throws Exception {
        assertThat(this.restTemplate.getForObject("http://localhost:" + port + "/index.html", String.class))
                .contains("INTERLIS model finder");
    }

    @Test
    public void search_Ok() throws Exception {
        assertThat(this.restTemplate.getForObject("http://localhost:" + port + "/search?query=wald", String.class))
                .contains("Wald");
    }
    
    @Test
    public void search_Restricted_Ok() throws Exception {
        assertThat(this.restTemplate.getForObject("http://localhost:" + port + "/search?query=wald&ilisite=models.geo.admin.ch", String.class))
                .contains("Wald");
    }

}
