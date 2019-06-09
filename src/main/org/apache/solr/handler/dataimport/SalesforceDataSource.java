package org.apache.solr.handler.dataimport;


import com.sforce.ws.ConnectorConfig;
import org.apache.solr.handler.dataimport.api.ApiClientFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

public class SalesforceDataSource extends DataSource<Iterator<Map<String, Object>>> {

    private static final String USER_NAME = "username";
    private static final String PASSWORD = "password";
    private static final String LOGIN_URL = "loginUrl";

    private Context context;

    @Override
    public void init(Context context, Properties properties) {
        this.context = context;
        String username = properties.getProperty(USER_NAME);
        String password = properties.getProperty(PASSWORD);
        String loginUrl = properties.getProperty(LOGIN_URL);

        ConnectorConfig config = new ConnectorConfig();
        if (loginUrl != null) {
            config.setAuthEndpoint(loginUrl);
        }
        config.setUsername(username);
        config.setPassword(password);

        ApiClientFactory.init(config);

    }

    @Override
    public Iterator<Map<String, Object>> getData(String query) {
        return ApiClientFactory.getApi(context).query(context, query, isQueryAll(query));
    }

    @Override
    public void close() {
        ApiClientFactory.getApi(context).close();
    }

    // look for "IsDeleted = True" in the query to determine if we should invoke connection.queryAll or connection.query
    private boolean isQueryAll(String query) {
        return query.replaceAll(" ", "").toUpperCase().contains("ISDELETED=TRUE");
    }

    void updateContext(Context context) {
        this.context = context;
    }
}
