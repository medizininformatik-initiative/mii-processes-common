package de.medizininformatik_initiative.processes.common.fhir.client.token;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OidcConfiguration
{
	private static final Logger logger = LoggerFactory.getLogger(OidcConfiguration.class);

	private final String issuer;
	private final String tokenEndpoint;

	@JsonCreator
	public OidcConfiguration(@JsonProperty("issuer") String issuer,
			@JsonProperty("token_endpoint") String tokenEndpoint)
	{
		this.issuer = issuer;
		this.tokenEndpoint = tokenEndpoint;
	}

	public String getIssuer()
	{
		return issuer;
	}

	public String getTokenEndpoint()
	{
		return tokenEndpoint;
	}

	public void validate(String issuerUrl, boolean lenient)
	{
		if (!Objects.equals(issuer, issuerUrl))
		{
			String message = "Issuer resolved in OIDC discovery does not match provided issuerUrl [issuer: " + issuer
					+ "; issuerUrl: " + issuerUrl + "]";

			if (lenient)
				logger.warn(message);
			else
				throw new IllegalStateException(message);
		}
	}
}
