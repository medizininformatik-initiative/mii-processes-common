package de.medizininformatik_initiative.processes.common.fhir.client.token;

import java.util.Objects;

import org.springframework.beans.factory.InitializingBean;

public class OAuth2TokenProvider implements TokenProvider, InitializingBean
{
	private final TokenClient tokenClient;

	private AccessToken token;

	public OAuth2TokenProvider(TokenClient tokenClient)
	{
		this.tokenClient = tokenClient;
	}

	@Override
	public void afterPropertiesSet()
	{
		Objects.requireNonNull(tokenClient, "tokenClient");
	}

	@Override
	public boolean isConfigured()
	{
		return tokenClient.isConfigured();
	}

	@Override
	public String getInfo()
	{
		return tokenClient.getInfo();
	}

	@Override
	public String getToken()
	{
		if (token == null || token.isExpired())
		{
			token = tokenClient.requestToken();
		}

		return token.get();
	}
}
