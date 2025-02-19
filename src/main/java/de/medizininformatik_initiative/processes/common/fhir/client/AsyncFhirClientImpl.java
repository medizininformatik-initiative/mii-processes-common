package de.medizininformatik_initiative.processes.common.fhir.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyStore;

import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import de.medizininformatik_initiative.processes.common.fhir.client.logging.DataLogger;
import de.medizininformatik_initiative.processes.common.fhir.client.token.TokenProvider;

public class AsyncFhirClientImpl extends AbstractHttpFhirClient implements AsyncFhirClient
{
	private static final Logger logger = LoggerFactory.getLogger(AsyncFhirClientImpl.class);

	private final int initialPollingIntervalMilliseconds;

	public AsyncFhirClientImpl(KeyStore trustStore, KeyStore keyStore, char[] keyStorePassword, int connectTimeout,
			int socketTimeout, String fhirServerBasicAuthUsername, String fhirServerBasicAuthPassword,
			String fhirServerBearerToken, TokenProvider fhirServerOAuth2TokenProvider, String fhirServerBase,
			String proxyUrl, String proxyUsername, String proxyPassword, int initialPollingIntervalMilliseconds,
			FhirContext fhirContext, String localIdentifierValue, DataLogger dataLogger)
	{
		super(trustStore, keyStore, keyStorePassword, connectTimeout, socketTimeout, fhirServerBasicAuthUsername,
				fhirServerBasicAuthPassword, fhirServerBearerToken, fhirServerOAuth2TokenProvider, fhirServerBase,
				proxyUrl, proxyUsername, proxyPassword, fhirContext, localIdentifierValue, dataLogger);

		this.initialPollingIntervalMilliseconds = initialPollingIntervalMilliseconds;
	}

	@Override
	public Resource search(String url)
	{
		HttpClient client = createClient();
		HttpRequest request = createBaseRequest(url).header("Prefer", "respond-async").GET().build();

		try
		{
			logger.debug("Async search for URL '{}' started", url);
			HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

			int currentPollingIntervalMilliseconds = initialPollingIntervalMilliseconds;
			while (response.statusCode() == HttpURLConnection.HTTP_ACCEPTED)
			{
				response.body().close();
				response = pollSearchResultAfterDelay(client, currentPollingIntervalMilliseconds, response.headers(),
						url);
				currentPollingIntervalMilliseconds = currentPollingIntervalMilliseconds * 2;
			}

			if (response.statusCode() == HttpURLConnection.HTTP_OK)
			{
				try (InputStream body = response.body())
				{
					return (Resource) getFhirContext().newJsonParser().parseResource(body);
				}
			}
			else
			{
				response.body().close();
				throw new RuntimeException(
						"Request for URL '" + url + "' failed - status code: " + response.statusCode());
			}
		}
		catch (Exception exception)
		{
			throw new RuntimeException("Async search for URL '" + url + "' failed - " + exception.getMessage(),
					exception);
		}
	}

	private HttpResponse<InputStream> pollSearchResultAfterDelay(HttpClient client, int pollingInterval,
			HttpHeaders headers, String url) throws IOException, InterruptedException
	{
		logger.debug("Async search for '{}' in-progress, checking result in {} milliseconds", url,
				initialPollingIntervalMilliseconds);
		Thread.sleep(pollingInterval);

		String location = headers.firstValue("Content-Location")
				.orElseThrow(() -> new RuntimeException("No Content-Location header returned"));
		String locationPath = location.substring(location.indexOf("__async-status"));

		HttpRequest request = createBaseRequest(locationPath).GET().build();

		return client.send(request, HttpResponse.BodyHandlers.ofInputStream());
	}
}
