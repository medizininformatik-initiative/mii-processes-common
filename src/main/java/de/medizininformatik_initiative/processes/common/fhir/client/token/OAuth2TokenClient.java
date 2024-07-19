package de.medizininformatik_initiative.processes.common.fhir.client.token;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.Base64;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.rwh.utils.crypto.io.CertificateReader;

public class OAuth2TokenClient implements TokenClient, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(OAuth2TokenClient.class);

	private static final String TLS = "TLS";

	private static final String HEADER_CONTENT_TYPE = "Content-Type";
	private static final String MIME_TYPE_FORM_URLENCODED = "application/x-www-form-urlencoded";
	private static final String HEADER_AUTHORIZATION = "Authorization";
	private static final String HEADER_AUTHORIZATION_PROXY = "Proxy-Authorization";
	private static final String PREFIX_BASIC_AUTH = "Basic ";
	private static final String GRANT_TYPE_CLIENT_CREDENTIALS = "grant_type=client_credentials";

	private final String issuerUrl;
	private final String clientId;
	private final String clientSecret;

	private final int connectTimeout;
	private final int socketTimeout;

	private final Path trustStorePath;

	private final String proxyUrl;
	private final String proxyUsername;
	private final String proxyPassword;

	private final ObjectMapper objectMapper;

	public OAuth2TokenClient(String issuerUrl, String clientId, String clientSecret, int connectTimeout,
			int socketTimeout, Path trustStorePath, String proxyUrl, String proxyUsername, String proxyPassword)
	{
		this(issuerUrl, clientId, clientSecret, connectTimeout, socketTimeout, trustStorePath, proxyUrl, proxyUsername,
				proxyPassword, new ObjectMapper());
	}

	public OAuth2TokenClient(String issuerUrl, String clientId, String clientSecret, int connectTimeout,
			int socketTimeout, Path trustStorePath, String proxyUrl, String proxyUsername, String proxyPassword,
			ObjectMapper objectMapper)
	{
		this.issuerUrl = issuerUrl;
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.connectTimeout = connectTimeout;
		this.socketTimeout = socketTimeout;
		this.trustStorePath = trustStorePath;
		this.proxyUrl = proxyUrl;
		this.proxyUsername = proxyUsername;
		this.proxyPassword = proxyPassword;
		this.objectMapper = objectMapper;
	}

	@Override
	public void afterPropertiesSet()
	{
		if (connectTimeout < 0)
			throw new IllegalArgumentException("connectTimeout < 0");

		if (socketTimeout < 0)
			throw new IllegalArgumentException("socketTimeout < 0");
	}

	@Override
	public boolean isConfigured()
	{
		return issuerUrl != null && clientId != null && clientSecret != null;
	}

	@Override
	public String getInfo()
	{
		return "[issuerUrl: " + issuerUrl + ", clientId: " + clientId + ", clientSecret: "
				+ (clientSecret != null ? "***" : "null") + ", trustStorePath: " + trustStorePath.toString()
				+ ", proxyUrl: " + proxyUrl + ", proxyUsername: " + proxyUsername + ", proxyPassword: "
				+ (proxyPassword != null ? "***" : "null") + "]";
	}

	@Override
	public AccessToken requestToken()
	{
		try
		{
			HttpClient client = createClient();
			HttpRequest request = createAccessTokenRequest();
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() == HttpURLConnection.HTTP_OK)
				return objectMapper.readValue(response.body(), AccessToken.class);
			else
				throw new RuntimeException("Could not retrieve access token, status code: " + response.statusCode());
		}
		catch (IOException | InterruptedException exception)
		{
			throw new RuntimeException(exception);
		}
	}

	private HttpClient createClient()
	{
		HttpClient.Builder builder = HttpClient.newBuilder();
		builder.connectTimeout(Duration.ofMillis(connectTimeout));

		configureProxy(builder);
		configureTrustStore(builder);

		return builder.build();
	}

	private void configureTrustStore(HttpClient.Builder builder)
	{
		if (trustStorePath != null)
		{
			logger.debug("Reading trust-store from {}", trustStorePath.toString());
			KeyStore trustStore = readTrustStore(trustStorePath);
			SSLContext sslContext = createSslContext(trustStore);
			builder.sslContext(sslContext);
		}
	}

	private SSLContext createSslContext(KeyStore trustStore)
	{
		try
		{
			TrustManagerFactory trustManagerFactory = TrustManagerFactory
					.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			trustManagerFactory.init(trustStore);

			SSLContext sslContext = SSLContext.getInstance(TLS);
			sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

			return sslContext;
		}
		catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException exception)
		{
			throw new RuntimeException(exception);
		}
	}

	private KeyStore readTrustStore(Path trustPath)
	{
		try
		{
			return CertificateReader.allFromCer(trustPath);
		}
		catch (NoSuchAlgorithmException | CertificateException | KeyStoreException | IOException exception)
		{
			throw new RuntimeException(exception);
		}
	}

	private void configureProxy(HttpClient.Builder builder)
	{
		if (proxyUrl != null)
		{
			URI uri = URI.create(proxyUrl);
			builder.proxy(ProxySelector.of(new InetSocketAddress(uri.getHost(), uri.getPort())));
		}
	}

	private HttpRequest createAccessTokenRequest()
	{
		HttpRequest.Builder builder = HttpRequest.newBuilder();
		builder.uri(URI.create(issuerUrl));
		builder.timeout(Duration.ofMillis(socketTimeout));

		configureAuthentication(builder);
		configureProxyAuthentication(builder);

		builder.header(HEADER_CONTENT_TYPE, MIME_TYPE_FORM_URLENCODED);
		builder.POST(HttpRequest.BodyPublishers.ofString(GRANT_TYPE_CLIENT_CREDENTIALS));

		return builder.build();
	}

	private void configureAuthentication(HttpRequest.Builder builder)
	{
		// Keycloak not sending WWW-Authenticate header for response code 401
		String credentials = getCredentials(clientId, clientSecret);
		builder.header(HEADER_AUTHORIZATION, PREFIX_BASIC_AUTH + credentials);
	}

	private void configureProxyAuthentication(HttpRequest.Builder builder)
	{
		// Proxy authentication using similar workaround as basic authentication
		if (proxyUrl != null && proxyUsername != null && proxyPassword != null)
		{
			String proxyCredentials = getCredentials(proxyUsername, proxyPassword);
			builder.setHeader(HEADER_AUTHORIZATION_PROXY, PREFIX_BASIC_AUTH + proxyCredentials);
		}
	}

	private String getCredentials(String username, String password)
	{
		String credentials = username + ":" + password;
		return Base64.getEncoder().encodeToString(credentials.getBytes());
	}
}
