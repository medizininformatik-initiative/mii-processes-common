package de.medizininformatik_initiative.processes.common.util;

public interface ConstantsBase
{
	String PROCESS_MII_NAME_BASE = "medizininformatik-initiativede_";
	String PROCESS_MII_URI_BASE = "http://medizininformatik-initiative.de/bpe/Process/";

	String NAMINGSYSTEM_MII_PROJECT_IDENTIFIER = "http://medizininformatik-initiative.de/sid/project-identifier";
	String NAMINGSYSTEM_DSF_ORGANIZATION_IDENTIFIER_MEDICAL_INFORMATICS_INITIATIVE_CONSORTIUM = "medizininformatik-initiative.de";

	String CODESYSTEM_DSF_ORGANIZATION_ROLE = "http://dsf.dev/fhir/CodeSystem/organization-role";
	String CODESYSTEM_DSF_ORGANIZATION_ROLE_VALUE_DMS = "DMS";
	String CODESYSTEM_DSF_ORGANIZATION_ROLE_VALUE_DIC = "DIC";
	String CODESYSTEM_DSF_ORGANIZATION_ROLE_VALUE_HRP = "HRP";

	String CODESYSTEM_MII_CRYPTOGRAPHY = "http://medizininformatik-initiative.de/fhir/CodeSystem/cryptography";
	String CODESYSTEM_MII_CRYPTOGRAPHY_VALUE_PUBLIC_KEY = "public-key";

	String CODESYSTEM_DATA_SET_STATUS = "http://medizininformatik-initiative.de/fhir/CodeSystem/data-set-status";
	String CODESYSTEM_DATA_SET_STATUS_VALUE_NOT_ALLOWED = "not-allowed";
	String CODESYSTEM_DATA_SET_STATUS_VALUE_NOT_REACHABLE = "not-reachable";
	String CODESYSTEM_DATA_SET_STATUS_VALUE_RECEIPT_MISSING = "receipt-missing";
	String CODESYSTEM_DATA_SET_STATUS_VALUE_RECEIPT_OK = "receipt-ok";
	String CODESYSTEM_DATA_SET_STATUS_VALUE_RECEIPT_ERROR = "receipt-error";
	String CODESYSTEM_DATA_SET_STATUS_VALUE_RECEIVE_OK = "receive-ok";
	String CODESYSTEM_DATA_SET_STATUS_VALUE_RECEIVE_ERROR = "receive-error";

	String EXTENSION_DATA_SET_STATUS_ERROR_URL = "http://medizininformatik-initiative.de/fhir/StructureDefinition/extension-data-set-status-error";

	int DSF_CLIENT_RETRY_6_TIMES = 6;
	long DSF_CLIENT_RETRY_INTERVAL_10SEC = 10000;
	long DSF_CLIENT_RETRY_INTERVAL_5MIN = 300000;
}
