package org.apache.solr.handler.dataimport.api;

import com.sforce.async.*;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectorConfig;
import org.apache.solr.handler.dataimport.Context;
import org.apache.solr.handler.dataimport.DataImporter;

import java.io.*;
import java.util.*;

public class BulkApi implements ApiClient {

    private BulkConnection bulkConnection;

    @Override
    public boolean login(ConnectorConfig config) {
        try {
            config.setCompression(true);
            config.setTraceMessage(true);
            config.setPrettyPrintXml(true);
            new PartnerConnection(config);
            String serviceEndpoint = config.getServiceEndpoint();
            String restEndpoint = serviceEndpoint.replace("/Soap/u", "/async");
            restEndpoint = restEndpoint.substring(0, restEndpoint.lastIndexOf("/"));
            config.setRestEndpoint(restEndpoint);
            bulkConnection = new BulkConnection(config);
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Error connecting to Salesforce instance", e);
        }
    }

    @Override
    public Iterator<Map<String, Object>> query(Context context, String query, boolean isQueryAll) {
        try {
            return doBulkQuery(context, query, isQueryAll);
        } catch (Exception e) {
            throw new RuntimeException("Error running bulk query", e);
        }
    }

    @Override
    public void close() {

    }

    public ResultIterator doBulkQuery(Context context, String query, boolean isQueryAll) throws Exception {
        JobInfo job = new JobInfo();
        job.setObject(context.getEntityAttribute("sobject"));
        job.setOperation(OperationEnum.query);
        job.setConcurrencyMode(ConcurrencyMode.Parallel);
        job.setContentType(ContentType.CSV);
        if (isQueryAll) {
            job.setOperation(OperationEnum.queryAll);
        }

        job = bulkConnection.createJob(job);
        assert job.getId() != null;
        job = bulkConnection.getJobStatus(job.getId());

        ByteArrayInputStream bout = new ByteArrayInputStream(query.getBytes());
        BatchInfo info = bulkConnection.createBatchFromStream(job, bout);

        String[] queryResults = null;

        for (int i = 0; i < 10000; i ++) {
            Thread.sleep(5000);
            info = bulkConnection.getBatchInfo(job.getId(), info.getId());

            if (info.getState() == BatchStateEnum.Completed) {
                QueryResultList list = bulkConnection.getQueryResultList(job.getId(), info.getId());
                queryResults = list.getResult();
                break;
            } else if (info.getState() == BatchStateEnum.Failed) {
                break;
            }
        }
        return new ResultIterator(context, job, info, queryResults);
    }

    public class ResultIterator extends AbstractResultIterator {

        private String[] queryResults = null;
        private int queryResultIndex = 0;
        private int processedCount = 0;
        private BufferedReader reader = null;
        private JobInfo job;
        private BatchInfo batchInfo;
        private String[] fields;
        private Context context;

        public ResultIterator(Context context, JobInfo job, BatchInfo batchInfo, String[] queryResults) {
            this.context = context;
            this.queryResults = queryResults;
            this.job = job;
            this.batchInfo = batchInfo;
        }

        @Override
        public boolean hasNext() {
            return processedCount < batchInfo.getNumberRecordsProcessed();
        }

        @Override
        public Map<String, Object> next() {
            if (reader == null) {
                initReader();
            }
            try {
                String line = reader.readLine();
                if (line == null) {
                    // reach the end of stream, increase index
                    reader.close();
                    queryResultIndex++;
                    initReader();
                    line = reader.readLine();
                }
                // first line is always field header
                String[] data = parseCsvRow(line);
                if (fields == null) {
                    fields = data;
                    line = reader.readLine();
                    data = parseCsvRow(line);
                }
                Map<String, Object> sobject = new HashMap<>(fields.length);
                for (int i = 0; i < fields.length; i ++) {
                    sobject.put(fields[i], data[i]);
                }
                Map<String, Object> dataMap = buildDataMap(sobject, context);
                processedCount ++;
                return dataMap;
            } catch(IOException e){
                throw new RuntimeException("Error reading line or closing reader", e);
            }
        }

        private String[] parseCsvRow(String line) {
            return Arrays.stream(line.split(",")).map(s -> s.substring(1, s.length() - 1)).toArray(String[]::new);
        }

        private void initReader() {
            String resultId = queryResults[queryResultIndex];
            try {
                InputStream is = bulkConnection.getQueryResultStream(job.getId(), batchInfo.getId(), resultId);
                reader = new BufferedReader(new InputStreamReader(is));
            } catch (AsyncApiException e) {
                throw new RuntimeException("Error getting query result stream", e);
            }
        }

        @Override
        Object getData(Object sobjectMap, String fieldName) {
            return ((Map<String, Object>)sobjectMap).get(fieldName);
        }
    }

}
