package org.apache.solr.handler.dataimport.api;

import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.QueryResult;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;
import org.apache.solr.handler.dataimport.Context;
import org.apache.solr.handler.dataimport.DataImporter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SoapApi implements ApiClient{

    private PartnerConnection connection;

    @Override
    public boolean login(ConnectorConfig config) {
        try {
            connection = new PartnerConnection(config);
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Error connecting to Salesforce instance", e);
        }
    }

    @Override
    public Iterator<Map<String, Object>> query(Context context, String query, boolean isQueryAll) {
        try {
            QueryResult result = isQueryAll ? connection.queryAll(query) : connection.query(query);
            return new ResultIterator(context, result);
        } catch (ConnectionException e) {
            throw new RuntimeException("Error querying Salesforce for data: " + query, e);
        }
    }

    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.logout();
            } catch (ConnectionException e) {
                // ignore
            }
        }
    }

    private class ResultIterator extends AbstractResultIterator {

        private QueryResult result;
        private int index;
        int indexInBatch;
        private String queryLocator;
        private Context context;

        public ResultIterator(Context context, QueryResult result) {
            this.context = context;
            this.result = result;
            this.index = 0;
            this.indexInBatch = 0;
            this.queryLocator = result.getQueryLocator();
        }

        @Override
        public boolean hasNext() {
            return index < result.getSize() || !result.isDone();
        }

        @Override
        public Map<String, Object> next() {
            if (indexInBatch == result.getRecords().length && this.queryLocator != null) {
                try {
                    result = connection.queryMore(this.queryLocator);
                    this.queryLocator = result.getQueryLocator();
                } catch (ConnectionException e) {
                    throw new RuntimeException("Error executing queryMore", e);
                }
                indexInBatch = 0;
            }
            SObject sobject = result.getRecords()[indexInBatch];
            indexInBatch++;
            index ++;
            Map<String, Object> dataMap = buildDataMap(sobject, context);
            return dataMap;
        }

        @Override
        Object getData(Object sobject, String fieldName) {
            Object value = sobject;
            String[] parts = fieldName.split("\\.");
            for (String part : parts) {
                value = ((SObject)value).getField(part);
                if (value == null) {
                    return null;
                }
            }
            return value;
        }
    }
}
