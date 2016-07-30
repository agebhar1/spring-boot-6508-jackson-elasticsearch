package com.github.agebhar1.service.elasticsearch;

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.elasticsearch.common.settings.Settings.settingsBuilder;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;
import static org.elasticsearch.search.sort.SortBuilders.fieldSort;
import static org.elasticsearch.search.sort.SortOrder.DESC;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.node.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.agebhar1.openweathermap.City;
import com.github.agebhar1.openweathermap.Forecast;

@Service
public class SampleIndex implements Closeable {
	
	private final static Logger logger = LoggerFactory.getLogger(SampleIndex.class);

	private final static String Index = "sample";

	private final Node node;
	private final ObjectMapper mapper;
	
	// http://stackoverflow.com/questions/27603570/elasticsearch-noshardavailableactionexception-after-startup
	private static void waitForYellowStatus(Node node) {
		logger.info("Wait for Elasticsearch Yellow node status.");
		node.client().admin().cluster().prepareHealth().setWaitForYellowStatus().execute().actionGet(5000);		
	}
	
	@Autowired
	public SampleIndex(final ElasticsearchConfiguration config) {
		
		logger.info("Create instance of class '{}' with {}.", getClass().getCanonicalName(), config);
		
		if (config == null) {
			throw new IllegalArgumentException("Elasticsearch embedded node configuration MUST not be null.");
		}
				
		node = nodeBuilder().settings(
				settingsBuilder()
					.put("path.home", config.getPath().getHome())
					.put("path.data", config.getPath().getData())
					.put("http.enabled", config.getHttp().isEnabled())
					.put("http.port", config.getHttp().getPort())
					.put("node.name", config.getNode().getName())
				)
				.local(true)
				.data(true)
				.node();
		
		waitForYellowStatus(node);
		
		final IndicesExistsResponse response = node.client().admin().indices().exists(new IndicesExistsRequest(Index)).actionGet();
		logger.info("Elasticsearch index '{}' exists: {}.", Index, response.isExists());
		
		if (!response.isExists()) {
			
			try {
				
				final CreateIndexResponse res = node.client().admin().indices().prepareCreate(Index).get();
				logger.info("Acknowledged on Elasticsearch index creation: {}", res.isAcknowledged());
				
				waitForYellowStatus(node);		
				
			} catch (final Exception e) {
				logger.error("Got excpetion of class '{}' with message '{}'.", e.getClass().getCanonicalName(), e.getMessage());
				throw new RuntimeException(e);
			}
			
		}
		
		mapper = new ObjectMapper();
		
	}
	
	public boolean index(final Forecast forecast) {
		try {
			
			logger.debug("Try to index forecast: '{}' for city: '{}'", forecast, forecast.getCity());
			
			final String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(forecast);
			final IndexResponse response = node.client().prepareIndex(Index, "forecast").setSource(json).execute().actionGet();

			logger.trace("Got response: {} on indexing forecast: {}.", response, json);
			if (response.getShardInfo().getFailed() > 0) {
				logger.error("Forecast {} could not be indexed. {}", json, response);
			}
			return response.getShardInfo().getFailed() > 0;
			
		} catch (final JsonProcessingException e) {
			
			logger.error("Got JSON processing exception on forecast: {}. {}", forecast, e.getMessage());
			return false;
			
		}		
	}
	
	public List<Forecast> fetchByCityId(int id) {
		
		final ObjectMapper mapper = new ObjectMapper();
		
		final SearchResponse response = node.client()
				.prepareSearch(Index)
				.setTypes("forecast")
				.setQuery(
					boolQuery()
						.filter(
							boolQuery()
								.must(termQuery("city.id", id))
						)
					)
				.addSort(fieldSort("dt").order(DESC))
				.get();
		
		return stream(response.getHits().spliterator(), true)
			.map(hit -> {
		
				Forecast forecast = null;
				try {
					final ObjectNode node = mapper.readValue(hit.getSourceAsString(), ObjectNode.class);
					final City city = City.of(node.at("/city/id").asInt(), node.at("/city/name").asText());
					forecast = Forecast.of(city, node.get("dt").asLong(), node.at("/main/temp_min").asDouble(), node.at("/main/temp_max").asDouble());
				} catch (IOException e) {
					logger.error("Got exception of type '{}' while deserialize forecast. {}", e.getClass().getCanonicalName(), e.getMessage());
				}
				return Optional.ofNullable(forecast);
			
			})
			.filter(Optional::isPresent)
			.map(Optional::get)
			.collect(toList());
			
	}

	@Override
	public void close() throws IOException {
		logger.info("Try to shutdown embedded Elasticsearch node.");
		node.close();
	}

}
