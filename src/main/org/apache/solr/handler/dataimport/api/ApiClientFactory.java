package org.apache.solr.handler.dataimport.api;

import com.sforce.ws.ConnectorConfig;
import org.apache.solr.handler.dataimport.Context;

public class ApiClientFactory {

    private static ApiClient soapApi;
    private static ApiClient bulkApi;

    public static void init(ConnectorConfig config) {
        soapApi = new SoapApi();
        soapApi.login(config);
        bulkApi = new BulkApi();
        bulkApi.login(config);
    }

    public static ApiClient getApi(Context context) {
        return ("DELTA_DUMP".equals(context.currentProcess()) || "FIND_DELTA".equals(context.currentProcess())) ?
                soapApi : bulkApi;
    }

}
