package de.medizininformatik_initiative.processes.common.fhir.client.token;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AccessToken
{
	private static final int BUFFER_10 = 10;

	private final String token;

	private final LocalDateTime expiresAt;

	@JsonCreator
	public AccessToken(@JsonProperty("access_token") String token, @JsonProperty("expires_in") int expiresIn)
	{
		this.token = token;
		this.expiresAt = LocalDateTime.now().plusSeconds(expiresIn);
	}

	public String get()
	{
		return token;
	}

	public boolean isExpired()
	{
		return LocalDateTime.now().plusSeconds(BUFFER_10).isAfter(expiresAt);
	}
}
