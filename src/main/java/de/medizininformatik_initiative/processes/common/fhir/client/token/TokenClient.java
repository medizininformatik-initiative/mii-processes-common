package de.medizininformatik_initiative.processes.common.fhir.client.token;

public interface TokenClient
{
	boolean isConfigured();

	AccessToken requestToken();
}
