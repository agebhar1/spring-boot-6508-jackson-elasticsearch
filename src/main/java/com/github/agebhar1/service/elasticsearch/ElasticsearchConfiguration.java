package com.github.agebhar1.service.elasticsearch;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "github.agebhar1.service.elasticsearch")
public class ElasticsearchConfiguration {

	@Data
	public static class Path {
		private String home;
		private String data;
	}
	
	@Data
	public static class Node {
		private String name;
	}
	
	@Data
	public static class Http {
		private boolean enabled;
		private int port;
	}
	
	private Path path;
	private Node node;
	private Http http;
	
}
