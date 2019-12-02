package com.rajanainart.common.data.nosql;

import com.rajanainart.common.data.provider.MemSqlDbProvider;
import com.rajanainart.common.helper.MiscHelper;
import com.rajanainart.common.rest.RestQueryConfig;
import com.rajanainart.common.rest.exception.RestConfigException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component("elasticsearch")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ElasticSearchNoSqlDataProvider extends MemSqlDbProvider implements BaseNoSqlDataProvider {

    private RestHighLevelClient client  = null;
    private RestQueryConfig queryConfig = null;

    public void open(NoSqlConfig noSqlConfig, RestQueryConfig queryConfig) {
        this.queryConfig = queryConfig;

        RestClientBuilder builder = getRestClientBuilder(noSqlConfig);
        client = new RestHighLevelClient(builder);
    }

    public void close() {
        try {
            if (client != null) client.close();
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static String getParsedIndexName(String input) {
        Date   today = new Date();
        String index = input.replace("_", "-");
        index = index.replace("yyyy", getDateFormat("yyyy").format(today));
        index = index.replace("yy", getDateFormat("yy").format(today));
        index = index.replace("MM", getDateFormat("MM").format(today));
        index = index.replace("dd", getDateFormat("dd").format(today));

        return index;
    }

    public static DateFormat getDateFormat(String format) {
        return new SimpleDateFormat(format);
    }

    public String bulkUpdate(List<Map<String, Object>> records) throws RestConfigException, IOException {
        RestQueryConfig.FieldConfig pk = validate(queryConfig, records);
        BulkRequest request = new BulkRequest();
        String      index   = getParsedIndexName(queryConfig.getTarget().toLowerCase(Locale.ENGLISH));

        for (Map<String, Object> record : records) {
            String id = String.valueOf(record.get(pk.getId()));
            IndexRequest  indexRequest     = new IndexRequest (index, id);
            UpdateRequest updateRequest    = new UpdateRequest(index, id);
            XContentBuilder contentBuilder = XContentFactory.jsonBuilder().startObject();
            for (RestQueryConfig.FieldConfig field : queryConfig.getFields()) {
                switch (field.getType()) {
                    case INTEGER:
                        contentBuilder.field(field.getTargetField(), MiscHelper.convertObjectToLong(record.get(field.getId())));
                        break;
                    case NUMERIC:
                        contentBuilder.field(field.getTargetField(), MiscHelper.convertObjectToDouble(record.get(field.getId())));
                        break;
                    default:
                        contentBuilder.field(field.getTargetField(), String.valueOf(record.get(field.getId())));
                        break;
                }
            }
            contentBuilder.endObject();

            indexRequest .source(contentBuilder);
            updateRequest.doc(contentBuilder).upsert(indexRequest);

            request.add(updateRequest);
        }
        BulkResponse response = client.bulk(request, RequestOptions.DEFAULT);
        return response.status().name();
    }

    public String bulkDelete(List<Map<String, Object>> records) throws RestConfigException, IOException {
        RestQueryConfig.FieldConfig pk = validate(queryConfig, records);

        BulkRequest request = new BulkRequest();
        for (Map<String, Object> record : records)
            request.add(new DeleteRequest(queryConfig.getTarget(), String.valueOf(record.get(pk.getId()))));
        BulkResponse response = client.bulk(request, RequestOptions.DEFAULT);
        return response.status().name();
    }

    public static RestClientBuilder getRestClientBuilder(NoSqlConfig config) {
        RestClientBuilder builder = null;
        if (!config.getUserName().isEmpty() && !config.getPassword().isEmpty()) {
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(config.getUserName(), config.getPassword()));

            builder = RestClient.builder(config.getHttpHosts())
                                .setHttpClientConfigCallback(httpClientBuilder ->
                                        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
                                );
            return builder;
        }
        else
            builder = RestClient.builder(config.getHttpHosts());
        return builder;
    }

    private RestQueryConfig.FieldConfig validate(RestQueryConfig config, List<Map<String, Object>> records) throws RestConfigException {
        RestQueryConfig.FieldConfig pk = config.getPKField();
        if (pk == null)
            throw new RestConfigException(String.format("PK field configuration does not exist in the config: %s", config.getId()));
        if (records.size() == 0 || (records.size() > 0 && !records.get(0).containsKey(pk.getId())))
            throw new RestConfigException(String.format("PK field %s does not exist in the resultset", pk.getId()));

        return pk;
    }
}
