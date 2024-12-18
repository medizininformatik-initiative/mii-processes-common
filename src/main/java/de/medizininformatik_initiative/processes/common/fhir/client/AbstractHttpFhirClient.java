package de.medizininformatik_initiative.processes.common.fhir.client;

import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.hl7.fhir.r4.model.CapabilityStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import de.medizininformatik_initiative.processes.common.fhir.client.logging.DataLogger;
import de.medizininformatik_initiative.processes.common.fhir.client.token.TokenProvider;

public abstract class AbstractHttpFhirClient implements FhirClient
{
	static
	{
		System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
	}

	private static final Logger logger = LoggerFactory.getLogger(AsyncFhirClientImpl.class);

	private final String fhirServerBase;

	private final KeyStore trustStore;
	private final KeyStore keyStore;
	private final char[] keyStorePassword;

	private final int connectTimeout;
	private final int socketTimeout;

	private final String fhirServerBasicAuthUsername;
	private final String fhirServerBasicAuthPassword;
	private final String fhirServerBearerToken;
	private final TokenProvider fhirServerOAuth2TokenProvider;

	private final String proxyUrl;
	private final String proxyUsername;
	private final String proxyPassword;

	private final FhirContext fhirContext;

	private final String localIdentifierValue;

	private final DataLogger dataLogger;

	public AbstractHttpFhirClient(KeyStore trustStore, KeyStore keyStore, char[] keyStorePassword, int connectTimeout,
			int socketTimeout, String fhirServerBasicAuthUsername, String fhirServerBasicAuthPassword,
			String fhirServerBearerToken, TokenProvider fhirServerOAuth2TokenProvider, String fhirServerBase,
			String proxyUrl, String proxyUsername, String proxyPassword, FhirContext fhirContext,
			String localIdentifierValue, DataLogger dataLogger)
	{
		this.trustStore = trustStore;
		this.keyStore = keyStore;
		this.keyStorePassword = keyStorePassword;

		this.connectTimeout = connectTimeout;
		this.socketTimeout = socketTimeout;

		this.fhirServerBasicAuthUsername = fhirServerBasicAuthUsername;
		this.fhirServerBasicAuthPassword = fhirServerBasicAuthPassword;
		this.fhirServerBearerToken = fhirServerBearerToken;
		this.fhirServerOAuth2TokenProvider = fhirServerOAuth2TokenProvider;

		this.fhirServerBase = fhirServerBase.endsWith("/") ? fhirServerBase : fhirServerBase + "/";

		this.proxyUrl = proxyUrl;
		this.proxyUsername = proxyUsername;
		this.proxyPassword = proxyPassword;

		this.fhirContext = fhirContext;

		this.localIdentifierValue = localIdentifierValue;

		this.dataLogger = dataLogger;
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
	public DataLogger getDataLogger()
	{
		return dataLogger;
	}

	@Override
	public void testConnection()
	{
		try
		{
			HttpClient client = createClient();
			HttpRequest request = createBaseRequest("metadata").GET().build();

			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() == HttpURLConnection.HTTP_OK)
			{
				CapabilityStatement statement = fhirContext.newJsonParser().parseResource(CapabilityStatement.class,
						response.body());
				logger.info("Connection test OK {} - {}", statement.getSoftware().getName(),
						statement.getSoftware().getVersion());
			}
			else
				throw new RuntimeException("Connection test failed - status code:" + response.statusCode());
		}
		catch (Exception exception)
		{
			throw new RuntimeException(exception);
		}
	}

	protected HttpClient createClient()
	{
		HttpClient.Builder builder = HttpClient.newBuilder();
		builder.connectTimeout(Duration.ofMillis(connectTimeout));

		configureProxy(builder);
		configureTrustStoreAndKeyStore(builder);

		return builder.build();
	}

	protected HttpRequest.Builder createBaseRequest(String path)
	{
		return createBaseRequest(path, Map.of());
	}

	protected HttpRequest.Builder createBaseRequest(String path, Map<String, String> headers)
	{
		HttpRequest.Builder builder = HttpRequest.newBuilder();
		builder.timeout(Duration.ofMillis(socketTimeout));

		path = path.startsWith("/") ? path.substring(1) : path;

		// URI throws exception if | not escaped
		path = path.replace("|", "%7C");
		URI uri = URI.create(fhirServerBase + path);

		builder.uri(uri);

		// will be overwritten if headers-map contains accept header
		builder.setHeader("Accept", "application/fhir+json");
		headers.forEach(builder::setHeader);

		configureAuthentication(builder);
		configureProxyAuthentication(builder);

		return builder;
	}

	private void configureProxy(HttpClient.Builder builder)
	{
		if (proxyUrl != null)
		{
			URI uri = URI.create(proxyUrl);
			builder.proxy(ProxySelector.of(new InetSocketAddress(uri.getHost(), uri.getPort())));

			logger.debug("Using proxy for connection with {host: {}, port: {}, username: {}, password: {}}",
					uri.getHost(), uri.getPort(), proxyUsername, proxyPassword != null ? "***" : "null");
		}
	}

	private void configureTrustStoreAndKeyStore(HttpClient.Builder builder)
	{
		if (trustStore != null)
		{
			SSLContext sslContext = createSslContext(trustStore, keyStore, keyStorePassword);
			builder.sslContext(sslContext);
		}
	}

	private SSLContext createSslContext(KeyStore trustStore, KeyStore keyStore, char[] keyStorePassword)
	{
		try
		{
			TrustManager[] trustManagers = null;
			if (trustStore != null)
			{
				TrustManagerFactory trustManagerFactory = TrustManagerFactory
						.getInstance(TrustManagerFactory.getDefaultAlgorithm());
				trustManagerFactory.init(trustStore);
				trustManagers = trustManagerFactory.getTrustManagers();
			}

			KeyManager[] keyManagers = null;
			if (keyStore != null)
			{
				KeyManagerFactory keyManagerFactory = KeyManagerFactory
						.getInstance(KeyManagerFactory.getDefaultAlgorithm());
				keyManagerFactory.init(keyStore, keyStorePassword);
				keyManagers = keyManagerFactory.getKeyManagers();
			}

			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(keyManagers, trustManagers, null);

			return sslContext;
		}
		catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException
				| UnrecoverableKeyException exception)
		{
			throw new RuntimeException(exception);
		}
	}

	private void configureAuthentication(HttpRequest.Builder builder)
	{
		if (fhirServerBasicAuthUsername != null && fhirServerBasicAuthPassword != null)
		{
			String credentials = encodeCredentials(fhirServerBasicAuthUsername, fhirServerBasicAuthPassword);
			builder.header("Authorization", "Basic " + credentials);
		}

		if (fhirServerOAuth2TokenProvider != null && fhirServerOAuth2TokenProvider.isConfigured())
		{
			String token = fhirServerOAuth2TokenProvider.getToken();
			builder.header("Authorization", "Bearer " + token);
		}

		if (fhirServerBearerToken != null)
		{
			builder.header("Authorization", "Bearer " + fhirServerBearerToken);
		}
	}

	private void configureProxyAuthentication(HttpRequest.Builder builder)
	{
		if (proxyUrl != null && proxyUsername != null & proxyPassword != null)
		{
			String credentials = encodeCredentials(proxyUsername, proxyPassword);
			builder.header("Proxy-Authorization", "Basic " + credentials);
		}
	}

	private String encodeCredentials(String username, String password)
	{
		String credentials = username + ":" + password;
		return Base64.getEncoder().encodeToString(credentials.getBytes());
	}
}
