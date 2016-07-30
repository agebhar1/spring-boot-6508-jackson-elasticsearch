package com.github.agebhar1.service.elasticsearch;

import static com.google.common.io.Files.createTempDir;
import static java.lang.String.format;
import static org.apache.commons.io.FileUtils.forceDelete;
import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.apache.commons.io.FilenameUtils.concat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.util.SocketUtils.findAvailableTcpPort;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.agebhar1.openweathermap.City;
import com.github.agebhar1.openweathermap.Forecast;
import com.github.agebhar1.service.elasticsearch.ElasticsearchConfiguration.Http;
import com.github.agebhar1.service.elasticsearch.ElasticsearchConfiguration.Node;
import com.github.agebhar1.service.elasticsearch.ElasticsearchConfiguration.Path;

public class SampleIndexTest {
	
	private static int port = 9200;
	private static File home;
	private static SampleIndex sample;
	
	@BeforeClass
	public static void setUp() throws IOException {
		
		home = createTempDir();
		final String data = concat(home.getAbsolutePath(), "data");
		forceMkdir(new File(data));
		
		/* spin up embedded Elasticsearch node */
		
		port = findAvailableTcpPort();
		
		final ElasticsearchConfiguration config = new ElasticsearchConfiguration();
		
		final Http http = new Http();
		http.setPort(port);
		http.setEnabled(true);		
		config.setHttp(http);
		
		final Node node = new Node();
		node.setName("sample-node-1");
		config.setNode(node);
		
		final Path path = new Path();
		path.setHome(home.getAbsolutePath());
		path.setData(data);
		config.setPath(path);
		
		sample = new SampleIndex(config);
		
		/* add some test data */
		final ObjectMapper mapper = new ObjectMapper();
		try(final InputStream json = SampleIndexTest.class.getClassLoader().getResourceAsStream("openweathermap~2879139.json")) {
			
			if (json == null) {
				throw new IllegalStateException(format("Could not load data '%s'.", "openweathermap~2879139.json"));
			}
			
			final ObjectNode n = mapper.readValue(json, ObjectNode.class);			
			final City city = City.of(n.at("/city/id").asInt(), n.at("/city/name").asText());
			
			n.at("/list").forEach(item -> {
				final Forecast forecast = Forecast.of(city, item.get("dt").asLong(), item.at("/main/temp_min").asDouble(), item.at("/main/temp_max").asDouble());
				sample.index(forecast);				
			});
			
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
		
	}
	
	@AfterClass
	public static void tearDown() throws IOException {
		sample.close();
		forceDelete(home);
	}

	@Test
	public void httpCall() {
		
		final RestTemplate template = new RestTemplate();
		final ResponseEntity<ObjectNode> entity = template.getForEntity("http://localhost:" + port + "/sample/forecast/_search", ObjectNode.class);
				
		assertThat(entity.getStatusCode(), is(equalTo(OK)));
		
	}
	
	@Test
	public void javaCall() {
		
		final List<Forecast> actual = sample.fetchByCityId(2879139);

		assertThat(actual, hasSize(greaterThan(0)));
		
	}

}
