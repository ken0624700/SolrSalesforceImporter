package org.apache.solr.handler.dataimport;

import java.util.Map;

public class SalesforceEntityProcessor extends EntityProcessorBase {

    public static final String QUERY = "query";
    public static final String DELTA_QUERY = "deltaQuery";
    public static final String DELTA_IMPORT_QUERY = "deltaImportQuery";
    public static final String DEL_PK_QUERY = "deletedPkQuery";

    private SalesforceDataSource dataSource;

    @Override
    public void init(Context context) {
        super.init(context);
        this.dataSource = (SalesforceDataSource)context.getDataSource();
        this.dataSource.updateContext(context);
    }

    @Override
    public Map<String, Object> nextRow() {
        if (rowIterator == null) {
            String query = this.getQuery();
            initQuery(context.replaceTokens(query));

        }
        return getNext();
    }

    @Override
    public Map<String, Object> nextModifiedRowKey() {
        return initPKQuery(DELTA_QUERY);
    }

    @Override
    public Map<String, Object> nextDeletedRowKey() {
        return initPKQuery(DEL_PK_QUERY);
    }

    private Map<String, Object> initPKQuery(String pkQueryName) {
        if (rowIterator == null) {
            String pkQuery = context.getEntityAttribute(pkQueryName);
            if (pkQuery == null) {
                return null;
            }
            initQuery(context.replaceTokens(pkQuery));
        }
        return getNext();

    }

    protected void initQuery(String query) {
        try {
            DataImporter.QUERY_COUNT.get().incrementAndGet();
            rowIterator = dataSource.getData(query);
            this.query = query;
        } catch (DataImportHandlerException e) {
            throw new RuntimeException(e);
        }
    }

    public String getQuery() {
        String queryString = this.context.getEntityAttribute(QUERY);
        if ("FULL_DUMP".equals(this.context.currentProcess())) {
            return queryString;
        } else if ("DELTA_DUMP".equals(this.context.currentProcess())) {
            return this.context.getEntityAttribute(DELTA_IMPORT_QUERY);

        } else {
            return null;
        }
    }

}
