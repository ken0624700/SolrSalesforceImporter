# Solr Salesforce Importer
This is a simple data import handler for importing data from Salesforce into Solr.

## Features
* Retrive data from Salesforce SObjects based on SOQL queries
* Map Salesforce SObject fields to Solr fields
* Full import using bulk API
* Delta import using SOAP API 
* Delete query
* Support for template value construct

## Classes

* SalesforceDataSource - Provides a Salesforce datasource
    * loginUrl     (*required* - Salesforce Partner API service end point)
    * username     (*required* - Salesforce login username)
    * password     (*required* - Salesforce login password concatenated with security token)
    
* SalesforceEntityProcessor - Use with SalesforceDataSource to query a Salesforce SObject
    * query (*required* - SOQL query to retrieve SObject data)
    * deltaQuery (*optional* - SOQL query that selects Id of SObject modified since last indexed time)
    * deltaImportQuery (*optional* - SOQL query to retrieve SObject data that matches Id from deltaQuery)
    * deletePkQuery (*optional* - SOQL query that selects Id of SObject deleted since last modified time)
    
* BulkAPI - Bulk API client

* SoapAPI - SOAP API client

## Installation
1. Building solr-salesforce-importer.jar
    - Check out latest version of SolrSalesforceImporter
    ```
        git clone git@github.com:/SolrSalesforceImporter.git
    ```
    - Build the jar file by running "ant" command under the SolrSalesforceImporter directory
     
3. Copy build/jar/solr-salesforce-importer-{version}.jar into your Solr instance's dist folder (e.g., /opt/solr/dist)
4. Copy lib/sforce-wsc-45.0.0-uber.jar into your Solr instance's dist folder
5. Copy lib/sfdc_partner_wsc.jar into your Solr instance's dist folder (refer to [this page](https://github.com/forcedotcom/wsc) for how to build this partner API jar file)
6. Add lib directives to your solrconfig.xml

```xml
    <lib path="../../dist/solr-salesforce-importer-{version}.jar" />
    <lib path="../../dist/sforce-wsc-45.0.0-uber.jar" />
    <lib path="../../dist/sfdc_partner_wsc.jar" />
```

## Usage
An sample data-config.xml
```xml
<?xml version="1.0" encoding="UTF-8" ?>
<dataConfig>
    <dataSource name="Salesforce" type="SalesforceDataSource" username="user@mycompany.com" password="MyPasswordMySecureToken" loginUrl="https://login.salesforce.com/services/Soap/u/44.0/"/>
    <document name="salesforce_records">
        <entity name="Contact"
            dataSource="Salesforce"
            processor="SalesforceEntityProcessor"
            sobject="Contact"
            query="SELECT Id, FirstName, LastName, Salutation, MailingAddress, Account.Name FROM Contact"
          deltaQuery="SELECT Id FROM Contact WHERE LastModifiedDate > ${dih.Contact.last_index_time}"
          deltaImportQuery="SELECT Id, FirstName, LastName, Salutation, MailingAddress FROM Contact WHERE Id = '${dih.delta.Id}'"
          deletedPkQuery="SELECT Id FROM Contact WHERE LastModifiedDate > ${dih.Contact.last_index_time} AND IsDeleted = true">
            <field name="id" column="Id" indexed="true" stored="true" />
            <field name="full_name" column="FirstName" value="${Salutation} ${FirstName} ${LastName}" indexed="true" stored="true" />
            <field name="mailing_address" column="MailingAddress" indexed="true" stored="true" />
            <field name="company" column="Account.Name" indexed="true" stored="true" />
      	</entity>
    </document>
 </dataConfig>
```
