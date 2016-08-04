#test case for https://github.com/spring-projects/spring-boot/issues/6508

* run `./mvnw test` which fails for HTTP communication w/ Elasticsearch with:
```
13:31:27.317 [main] DEBUG org.springframework.web.client.RestTemplate - Created GET request for "http://localhost:18020/sample/forecast/_search"
13:31:27.318 [main] DEBUG org.springframework.web.client.RestTemplate - Setting request Accept header to [application/json, application/*+json]
13:31:27.351 [elasticsearch[sample-node-1][search][T#2]] WARN rest.suppressed - path: /sample/forecast/_search, params: {index=sample, type=forecast}
java.lang.NoSuchMethodError: com.fasterxml.jackson.core.base.GeneratorBase.getOutputContext()Lcom/fasterxml/jackson/core/json/JsonWriteContext;
        at org.elasticsearch.common.xcontent.json.JsonXContentGenerator.writeEndRaw(JsonXContentGenerator.java:327)
        at org.elasticsearch.common.xcontent.json.JsonXContentGenerator.writeRawField(JsonXContentGenerator.java:368)
        at org.elasticsearch.common.xcontent.XContentBuilder.rawField(XContentBuilder.java:914)
        at org.elasticsearch.common.xcontent.XContentHelper.writeRawField(XContentHelper.java:378)
        at org.elasticsearch.search.internal.InternalSearchHit.toXContent(InternalSearchHit.java:476)
        at org.elasticsearch.search.internal.InternalSearchHits.toXContent(InternalSearchHits.java:184)
        at org.elasticsearch.search.internal.InternalSearchResponse.toXContent(InternalSearchResponse.java:111)
        at org.elasticsearch.action.search.SearchResponse.toXContent(SearchResponse.java:195)
        at org.elasticsearch.rest.action.support.RestStatusToXContentListener.buildResponse(RestStatusToXContentListener.java:43)
        at org.elasticsearch.rest.action.support.RestStatusToXContentListener.buildResponse(RestStatusToXContentListener.java:38)
        at org.elasticsearch.rest.action.support.RestStatusToXContentListener.buildResponse(RestStatusToXContentListener.java:30)
        at org.elasticsearch.rest.action.support.RestResponseListener.processResponse(RestResponseListener.java:43)
        at org.elasticsearch.rest.action.support.RestActionListener.onResponse(RestActionListener.java:49)
        at org.elasticsearch.action.support.TransportAction$1.onResponse(TransportAction.java:89)
        at org.elasticsearch.action.support.TransportAction$1.onResponse(TransportAction.java:85)
        at org.elasticsearch.action.search.SearchQueryThenFetchAsyncAction$2.doRun(SearchQueryThenFetchAsyncAction.java:138)
        at org.elasticsearch.common.util.concurrent.AbstractRunnable.run(AbstractRunnable.java:37)
        at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1142)
        at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617)
        at java.lang.Thread.run(Thread.java:745)
```
* run `./mvn test -Pv2.6.6` which manage Jackson version back to 2.6.6, all tests passed
* run `./mvn test -Pv2.7.6` which manage Jackson version back to 2.7.6, all tests passed
