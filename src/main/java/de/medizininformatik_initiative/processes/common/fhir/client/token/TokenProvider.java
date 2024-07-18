package de.medizininformatik_initiative.processes.common.fhir.client.token;

public interface TokenProvider
{
	boolean isConfigured();

	String getToken();
}
