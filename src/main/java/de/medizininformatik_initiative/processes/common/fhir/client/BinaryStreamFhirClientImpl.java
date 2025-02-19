package de.medizininformatik_initiative.processes.common.fhir.client;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyStore;
import java.util.Map;
import java.util.Optional;

import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import de.medizininformatik_initiative.processes.common.fhir.client.logging.DataLogger;
import de.medizininformatik_initiative.processes.common.fhir.client.token.TokenProvider;

public class BinaryStreamFhirClientImpl extends AbstractHttpFhirClient implements BinaryStreamFhirClient
{
	private static final Logger logger = LoggerFactory.getLogger(AsyncFhirClientImpl.class);

	public BinaryStreamFhirClientImpl(KeyStore trustStore, KeyStore keyStore, char[] keyStorePassword,
			int connectTimeout, int socketTimeout, String fhirServerBasicAuthUsername,
			String fhirServerBasicAuthPassword, String fhirServerBearerToken,
			TokenProvider fhirServerOAuth2TokenProvider, String fhirServerBase, String proxyUrl, String proxyUsername,
			String proxyPassword, FhirContext fhirContext, String localIdentifierValue, DataLogger dataLogger)
	{
		super(trustStore, keyStore, keyStorePassword, connectTimeout, socketTimeout, fhirServerBasicAuthUsername,
				fhirServerBasicAuthPassword, fhirServerBearerToken, fhirServerOAuth2TokenProvider, fhirServerBase,
				proxyUrl, proxyUsername, proxyPassword, fhirContext, localIdentifierValue, dataLogger);
	}

	@Override
	public InputStream read(IdType idType, String mimeType)
	{
		if (!ResourceType.Binary.name().equals(idType.getResourceType()))
			throw new UnsupportedOperationException(
					"Expected resource type 'Binary' but found resource type '" + idType.getResourceType() + "'");

		String unqualifiedId = idType.toUnqualified().getValue();

		HttpClient client = createClient();
		HttpRequest request = createBaseRequest(unqualifiedId, Map.of("Accept", mimeType)).GET().build();

		try
		{
			logger.debug("Reading Binary with id '{}' as stream", unqualifiedId);
			HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

			if (response.statusCode() == HttpURLConnection.HTTP_OK)
				return response.body();
			else
			{
				response.body().close();
				throw new RuntimeException("Reading Binary with id '" + unqualifiedId
						+ "' as stream failed - status code: " + response.statusCode());
			}
		}
		catch (Exception exception)
		{
			throw new RuntimeException("Reading Binary with id '" + unqualifiedId + "' as stream failed", exception);
		}
	}

	@Override
	public MethodOutcome create(InputStream stream, String mimeType)
	{
		HttpClient client = createClient();
		HttpRequest request = createBaseRequest("Binary", Map.of("Content-Type", mimeType))
				.POST(HttpRequest.BodyPublishers.ofInputStream(() -> stream)).build();

		try
		{
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			Optional<String> location = response.headers().firstValue("Location");

			if (location.isPresent())
				return new MethodOutcome(new IdType(location.get()), true);
			else
				throw new RuntimeException("Creating Binary as stream failed - " + response.body());
		}
		catch (Exception exception)
		{
			throw new RuntimeException("Saving Binary as stream failed", exception);
		}
	}
}
