package org.apache.solr.handler.dataimport.api;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.handler.dataimport.Context;
import org.apache.solr.handler.dataimport.DataImporter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public abstract class AbstractResultIterator implements Iterator<Map<String, Object>> {

    /**
     * Given a value template, stamp it with data values
     * @param template
     * @return
     */
    String stampValues(String template, Object sobjectMap) {
        String[] tokens = StringUtils.substringsBetween(template, "${", "}");
        if (tokens != null) {
            for (String token : tokens) {
                Object data = this.getData(sobjectMap, token);
                template = template.replace("${" + token + "}", data == null ? null : data.toString());
            }
        }
        return template;
    }

    Map<String, Object> buildDataMap(Object sobjectMap, Context context) {
        Map<String, Object> dataMap = new HashMap<>();
        for (Map<String, String> fieldMap : context.getAllEntityFields()) {
            String sobjectFieldName = fieldMap.get(DataImporter.COLUMN);
            String columnFieldName = fieldMap.get(DataImporter.NAME);
            Object value = fieldMap.get("value");
            if (value == null || "FIND_DELTA".equals(context.currentProcess())) {
                value = getData(sobjectMap, sobjectFieldName);
            } else {
                value = stampValues(value.toString(), sobjectMap);
            }
            dataMap.put(columnFieldName, value);
        }
        return dataMap;
    }

    abstract Object getData(Object sobjectMap, String fieldName);
}
