package de.medizininformatik_initiative.processes.common.fhir.client;

import static ca.uhn.fhir.rest.api.Constants.HEADER_PREFER;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.IRestfulClientFactory;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.gclient.IReadTyped;
import de.medizininformatik_initiative.processes.common.fhir.client.interceptor.OAuth2Interceptor;
import de.medizininformatik_initiative.processes.common.fhir.client.logging.DataLogger;
import de.medizininformatik_initiative.processes.common.fhir.client.logging.HapiClientLogger;
import de.medizininformatik_initiative.processes.common.fhir.client.token.TokenProvider;

public class FhirClientImpl implements FhirClient
{
	private static final Logger logger = LoggerFactory.getLogger(FhirClientImpl.class);

	private final IRestfulClientFactory clientFactory;

	private final String fhirServerBase;

	private final String fhirServerBasicAuthUsername;
	private final String fhirServerBasicAuthPassword;
	private final String fhirServerBearerToken;
	private final TokenProvider fhirServerOAuth2TokenProvider;

	private final boolean hapiClientVerbose;

	private final FhirContext fhirContext;

	private final String localIdentifierValue;

	private final DataLogger dataLogger;

	public FhirClientImpl(KeyStore trustStore, KeyStore keyStore, char[] keyStorePassword, int connectTimeout,
			int socketTimeout, int connectionRequestTimeout, String fhirServerBasicAuthUsername,
			String fhirServerBasicAuthPassword, String fhirServerBearerToken,
			TokenProvider fhirServerOAuth2TokenProvider, String fhirServerBase, String proxyUrl, String proxyUsername,
			String proxyPassword, boolean hapiClientVerbose, FhirContext fhirContext, String localIdentifierValue,
			DataLogger dataLogger)
	{
		clientFactory = createClientFactory(trustStore, keyStore, keyStorePassword, connectTimeout, socketTimeout,
				connectionRequestTimeout);

		this.fhirServerBase = fhirServerBase;

		this.fhirServerBasicAuthUsername = fhirServerBasicAuthUsername;
		this.fhirServerBasicAuthPassword = fhirServerBasicAuthPassword;
		this.fhirServerBearerToken = fhirServerBearerToken;
		this.fhirServerOAuth2TokenProvider = fhirServerOAuth2TokenProvider;

		configureProxy(clientFactory, proxyUrl, proxyUsername, proxyPassword);

		this.hapiClientVerbose = hapiClientVerbose;

		this.fhirContext = fhirContext;

		this.localIdentifierValue = localIdentifierValue;

		this.dataLogger = dataLogger;
	}

	private void configureProxy(IRestfulClientFactory clientFactory, String proxyUrl, String proxyUsername,
			String proxyPassword)
	{
		if (proxyUrl != null && !proxyUrl.isBlank())
		{
			try
			{
				URL url = new URL(proxyUrl);
				clientFactory.setProxy(url.getHost(), url.getPort());
				clientFactory.setProxyCredentials(proxyUsername, proxyPassword);

				logger.debug("Using proxy for FHIR server connection with {host: {}, port: {}, username: {}}",
						url.getHost(), url.getPort(), proxyUsername);
			}
			catch (MalformedURLException e)
			{
				logger.error("Could not configure proxy", e);
			}
		}
	}

	protected ApacheRestfulClientFactoryWithTlsConfig createClientFactory(KeyStore trustStore, KeyStore keyStore,
			char[] keyStorePassword, int connectTimeout, int socketTimeout, int connectionRequestTimeout)
	{
		FhirContext fhirContext = FhirContext.forR4();
		ApacheRestfulClientFactoryWithTlsConfig hapiClientFactory = new ApacheRestfulClientFactoryWithTlsConfig(
				fhirContext, trustStore, keyStore, keyStorePassword);
		hapiClientFactory.setServerValidationMode(ServerValidationModeEnum.NEVER);

		hapiClientFactory.setConnectTimeout(connectTimeout);
		hapiClientFactory.setSocketTimeout(socketTimeout);
		hapiClientFactory.setConnectionRequestTimeout(connectionRequestTimeout);

		fhirContext.setRestfulClientFactory(hapiClientFactory);
		return hapiClientFactory;
	}

	private void configuredWithBasicAuth(IGenericClient client)
	{
		if (fhirServerBasicAuthUsername != null && fhirServerBasicAuthPassword != null)
			client.registerInterceptor(
					new BasicAuthInterceptor(fhirServerBasicAuthUsername, fhirServerBasicAuthPassword));
	}

	private void configuredWithBearerTokenAuth(IGenericClient client)
	{
		if (fhirServerBearerToken != null)
			client.registerInterceptor(new BearerTokenAuthInterceptor(fhirServerBearerToken));
	}

	private void configuredWithOAuth2(IGenericClient client)
	{
		if (fhirServerOAuth2TokenProvider != null && fhirServerOAuth2TokenProvider.isConfigured())
			client.registerInterceptor(new OAuth2Interceptor(fhirServerOAuth2TokenProvider));
	}

	private void configureLoggingInterceptor(IGenericClient client)
	{
		if (hapiClientVerbose)
		{
			LoggingInterceptor loggingInterceptor = new LoggingInterceptor(true);
			loggingInterceptor.setLogger(new HapiClientLogger(logger));
			client.registerInterceptor(loggingInterceptor);
		}
	}

	@Override
	public String getLocalIdentifierValue()
	{
		return localIdentifierValue;
	}

	@Override
	public FhirContext getFhirContext()
	{
		return fhirContext;
	}

	@Override
	public String getFhirBaseUrl()
	{
		return fhirServerBase;
	}

	@Override
	public IGenericClient getGenericFhirClient()
	{
		IGenericClient client = clientFactory.newGenericClient(fhirServerBase);

		configuredWithBasicAuth(client);
		configuredWithBearerTokenAuth(client);
		configuredWithOAuth2(client);
		configureLoggingInterceptor(client);

		return client;
	}

	@Override
	public void testConnection()
	{
		CapabilityStatement statement = getGenericFhirClient().capabilities().ofType(CapabilityStatement.class)
				.execute();

		logger.info("Connection test OK {} - {}", statement.getSoftware().getName(),
				statement.getSoftware().getVersion());
	}

	@Override
	public Resource read(IdType idType)
	{
		Resource resource = doRead(idType);
		dataLogger.logResource("Read Resource from url '" + idType.getValue() + "'", resource);

		return resource;
	}

	private Resource doRead(IdType idType)
	{
		IReadTyped<IBaseResource> readTyped = getGenericFhirClient().read().resource(idType.getResourceType());

		if (idType.hasVersionIdPart())
			return (Resource) readTyped.withIdAndVersion(idType.getIdPart(), idType.getVersionIdPart()).execute();
		else
			return (Resource) readTyped.withId(idType.getIdPart()).execute();
	}

	@Override
	public Binary readBinary(IdType idType)
	{
		Binary binary = getGenericFhirClient().read().resource(Binary.class).withId(idType.getIdPart()).execute();

		dataLogger.logResource("Read Binary from url '" + idType.getValue() + "'", binary);

		return binary;
	}

	@Override
	public Resource search(String url)
	{
		return (Resource) getGenericFhirClient().search().byUrl(url).execute();
	}

	@Override
	public Bundle searchDocumentReferences(String system, String code)
	{
		Bundle bundle = getGenericFhirClient().search().forResource(DocumentReference.class)
				.where(DocumentReference.IDENTIFIER.exactly().systemAndIdentifier(system, code))
				.returnBundle(Bundle.class).execute();

		dataLogger.logResource("DocumentReference Search-Response Bundle based on system|code=" + system + "|" + code,
				bundle);

		return bundle;
	}

	@Override
	public Bundle executeTransaction(Bundle bundle)
	{
		dataLogger.logResource("Executing Transaction Bundle", bundle);

		Bundle response = getGenericFhirClient().transaction().withBundle(bundle)
				.withAdditionalHeader(HEADER_PREFER, "handling=strict").execute();

		dataLogger.logResource("Transaction Bundle Response", response);

		return response;
	}

	@Override
	public Bundle executeBatch(Bundle bundle)
	{
		dataLogger.logResource("Executing Batch Bundle", bundle);

		Bundle response = getGenericFhirClient().transaction().withBundle(bundle)
				.withAdditionalHeader(HEADER_PREFER, "handling=strict").execute();

		dataLogger.logResource("Batch Bundle Response", response);

		return response;
	}

	@Override
	public MethodOutcome create(Resource resource)
	{
		dataLogger.logResource("Creating " + resource.getResourceType().name(), resource);

		MethodOutcome outcome = getGenericFhirClient().create().resource(resource).execute();

		dataLogger.logMethodOutcome("Create Task MethodOutcome", outcome);

		return outcome;
	}
}
