package org.apache.solr.handler.dataimport.api;

import com.sforce.ws.ConnectorConfig;
import org.apache.solr.handler.dataimport.Context;

import java.util.Iterator;
import java.util.Map;

public interface ApiClient {

    boolean login(ConnectorConfig config);

    Iterator<Map<String, Object>> query(Context context, String query, boolean isQueryAll);

    void close();
}
