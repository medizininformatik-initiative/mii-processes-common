package de.medizininformatik_initiative.processes.common.fhir.client.token;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OidcConfiguration
{
	private final String tokenEndpoint;

	@JsonCreator
	public OidcConfiguration(@JsonProperty("token_endpoint") String tokenEndpoint)
	{
		this.tokenEndpoint = tokenEndpoint;
	}

	public String getTokenEndpoint()
	{
		return tokenEndpoint;
	}
}
