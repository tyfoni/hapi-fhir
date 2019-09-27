package ca.uhn.fhir.jpa.search.elastic;

import org.hibernate.search.cfg.Environment;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchIndexStatus;
import org.hibernate.search.elasticsearch.cfg.IndexSchemaManagementStrategy;

import java.util.Properties;

/**
 * This class is used to inject appropriate properties into a hibernate
 * Properties object being used to create an entitymanager for a HAPI
 * FHIR JPA server.
 */
public class ElasticsearchHibernatePropertiesBuilder {

	private ElasticsearchIndexStatus myRequiredIndexStatus = ElasticsearchIndexStatus.YELLOW;
	private String myRestUrl;
	private IndexSchemaManagementStrategy myIndexSchemaManagementStrategy = IndexSchemaManagementStrategy.CREATE;
	private long myIndexManagementWaitTimeoutMillis = 10000L;
	private boolean myDebugRefreshAfterWrite = false;
	private boolean myDebugPrettyPrintJsonLog = false;

	public void apply(Properties theProperties) {

		// Don't use the Lucene properties as they conflict
		theProperties.remove("hibernate.search.model_mapping");

		// the below properties are used for ElasticSearch integration
		theProperties.put("hibernate.search.default." + Environment.INDEX_MANAGER_IMPL_NAME, "elasticsearch");
		theProperties.put("hibernate.search." + ElasticsearchEnvironment.ANALYSIS_DEFINITION_PROVIDER, ElasticsearchMappingProvider.class.getName());

		theProperties.put("hibernate.search.default.elasticsearch.host", myRestUrl);
		theProperties.put("hibernate.search.default." + ElasticsearchEnvironment.INDEX_SCHEMA_MANAGEMENT_STRATEGY, myIndexSchemaManagementStrategy.getExternalName());
		theProperties.put("hibernate.search.default." + ElasticsearchEnvironment.INDEX_MANAGEMENT_WAIT_TIMEOUT, Long.toString(myIndexManagementWaitTimeoutMillis));
		theProperties.put("hibernate.search.default." + ElasticsearchEnvironment.REQUIRED_INDEX_STATUS, myRequiredIndexStatus.getElasticsearchString());

		// Only for unit tests
		theProperties.put("hibernate.search.default." + ElasticsearchEnvironment.REFRESH_AFTER_WRITE, Boolean.toString(myDebugRefreshAfterWrite));
		theProperties.put("hibernate.search." + ElasticsearchEnvironment.LOG_JSON_PRETTY_PRINTING, Boolean.toString(myDebugPrettyPrintJsonLog));

	}

	public ElasticsearchHibernatePropertiesBuilder setRequiredIndexStatus(ElasticsearchIndexStatus theRequiredIndexStatus) {
		myRequiredIndexStatus = theRequiredIndexStatus;
		return this;
	}

	public ElasticsearchHibernatePropertiesBuilder setRestUrl(String theRestUrl) {
		myRestUrl = theRestUrl;
		return this;
	}

	public ElasticsearchHibernatePropertiesBuilder setIndexSchemaManagementStrategy(IndexSchemaManagementStrategy theIndexSchemaManagementStrategy) {
		myIndexSchemaManagementStrategy = theIndexSchemaManagementStrategy;
		return this;
	}

	public ElasticsearchHibernatePropertiesBuilder setIndexManagementWaitTimeoutMillis(long theIndexManagementWaitTimeoutMillis) {
		myIndexManagementWaitTimeoutMillis = theIndexManagementWaitTimeoutMillis;
		return this;
	}

	public ElasticsearchHibernatePropertiesBuilder setDebugRefreshAfterWrite(boolean theDebugRefreshAfterWrite) {
		myDebugRefreshAfterWrite = theDebugRefreshAfterWrite;
		return this;
	}

	public ElasticsearchHibernatePropertiesBuilder setDebugPrettyPrintJsonLog(boolean theDebugPrettyPrintJsonLog) {
		myDebugPrettyPrintJsonLog = theDebugPrettyPrintJsonLog;
		return this;
	}


}