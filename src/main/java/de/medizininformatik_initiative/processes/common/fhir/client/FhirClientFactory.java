package de.medizininformatik_initiative.processes.common.fhir.client;

import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Objects;
import java.util.UUID;

import org.bouncycastle.pkcs.PKCSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ca.uhn.fhir.context.FhirContext;
import de.medizininformatik_initiative.processes.common.fhir.client.logging.DataLogger;
import de.medizininformatik_initiative.processes.common.fhir.client.token.TokenProvider;
import de.rwh.utils.crypto.CertificateHelper;
import de.rwh.utils.crypto.io.CertificateReader;
import de.rwh.utils.crypto.io.PemIo;

public class FhirClientFactory implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(FhirClientFactory.class);

	private final Path trustStorePath;
	private final Path certificatePath;
	private final Path privateKeyPath;
	private final char[] privateKeyPassword;

	private final int connectTimeout;
	private final int socketTimeout;
	private final int connectionRequestTimeout;

	private final String fhirServerBase;
	private final String fhirServerBasicAuthUsername;
	private final String fhirServerBasicAuthPassword;
	private final String fhirServerBearerToken;
	private final TokenProvider fhirServerOAuth2TokenProvider;

	private final String proxyUrl;
	private final String proxyUsername;
	private final String proxyPassword;

	private final boolean hapiClientVerbose;

	private final int initialPollingIntervalMilliseconds;

	private final FhirContext fhirContext;

	private final String localIdentifierValue;

	private final DataLogger dataLogger;

	public FhirClientFactory(Path trustStorePath, Path certificatePath, Path privateKeyPath, char[] privateKeyPassword,
			int connectTimeout, int socketTimeout, int connectionRequestTimeout, String fhirServerBase,
			String fhirServerBasicAuthUsername, String fhirServerBasicAuthPassword, String fhirServerBearerToken,
			TokenProvider fhirServerOAuth2TokenProvider, String proxyUrl, String proxyUsername, String proxyPassword,
			boolean hapiClientVerbose, int initialPollingIntervalMilliseconds, FhirContext fhirContext,
			String localIdentifierValue, DataLogger dataLogger)
	{
		this.trustStorePath = trustStorePath;
		this.certificatePath = certificatePath;
		this.privateKeyPath = privateKeyPath;
		this.privateKeyPassword = privateKeyPassword;

		this.connectTimeout = connectTimeout;
		this.socketTimeout = socketTimeout;
		this.connectionRequestTimeout = connectionRequestTimeout;

		this.fhirServerBase = fhirServerBase;
		this.fhirServerBasicAuthUsername = fhirServerBasicAuthUsername;
		this.fhirServerBasicAuthPassword = fhirServerBasicAuthPassword;
		this.fhirServerBearerToken = fhirServerBearerToken;
		this.fhirServerOAuth2TokenProvider = fhirServerOAuth2TokenProvider;

		this.proxyUrl = proxyUrl;
		this.proxyUsername = proxyUsername;
		this.proxyPassword = proxyPassword;

		this.hapiClientVerbose = hapiClientVerbose;

		this.initialPollingIntervalMilliseconds = initialPollingIntervalMilliseconds;

		this.fhirContext = fhirContext;

		this.localIdentifierValue = localIdentifierValue;

		this.dataLogger = dataLogger;
	}

	@Override
	public void afterPropertiesSet()
	{
		Objects.requireNonNull(fhirServerBase, "fhirServerBase");

		if (connectTimeout < 0)
			throw new IllegalArgumentException("connectTimeout < 0");

		if (socketTimeout < 0)
			throw new IllegalArgumentException("socketTimeout < 0");

		if (connectionRequestTimeout < 0)
			throw new IllegalArgumentException("connectRequestTimeout < 0");

		Objects.requireNonNull(fhirContext, "fhirContext");
		Objects.requireNonNull(localIdentifierValue, "localIdentifierValue");
	}

	public void testConnection()
	{
		try
		{
			logger.info(
					"Testing connection to FHIR server with {trustStorePath: {}, certificatePath: {}, privateKeyPath: {}, privateKeyPassword: {},"
							+ " basicAuthUsername: {}, basicAuthPassword: {}, bearerToken: {}, oauth2Provider: {}, serverBase: {}, proxyUrl: {}, proxyUsername: {}, proxyPassword: {}}",
					trustStorePath, certificatePath, privateKeyPath, privateKeyPassword != null ? "***" : "null",
					fhirServerBasicAuthUsername, fhirServerBasicAuthPassword != null ? "***" : "null",
					fhirServerBearerToken != null ? "***" : "null",
					fhirServerOAuth2TokenProvider != null ? fhirServerOAuth2TokenProvider.getInfo() : "null",
					fhirServerBase, proxyUrl, proxyUsername, proxyPassword != null ? "***" : "null");

			getStandardFhirClient().testConnection();
			getAsyncFhirClient().testConnection();
			getBinaryStreamFhirClient().testConnection();
		}
		catch (Exception e)
		{
			logger.error("Error while testing connection to FHIR server", e);
		}
	}

	public StandardFhirClient getStandardFhirClient()
	{
		if (configured())
			return createStandardFhirClient();
		else
			throw new RuntimeException("Configuration error: FHIR server base url not set");
	}

	public AsyncFhirClient getAsyncFhirClient()
	{
		if (configured())
			return createAsyncFhirClient();
		else
			throw new RuntimeException("Configuration error: FHIR server base url not set");
	}

	public BinaryStreamFhirClient getBinaryStreamFhirClient()
	{
		if (configured())
			return createBinaryStreamClient();
		else
			throw new RuntimeException("Configuration error: FHIR server base url not set");
	}

	private boolean configured()
	{
		return fhirServerBase != null && !fhirServerBase.isBlank();
	}

	protected StandardFhirClient createStandardFhirClient()
	{
		KeyStore trustStore = readTrustStore();

		char[] keyStorePassword = UUID.randomUUID().toString().toCharArray();
		KeyStore keyStore = readKeyStore(keyStorePassword);

		return new StandardFhirClientImpl(trustStore, keyStore, keyStorePassword, connectTimeout, socketTimeout,
				connectionRequestTimeout, fhirServerBasicAuthUsername, fhirServerBasicAuthPassword,
				fhirServerBearerToken, fhirServerOAuth2TokenProvider, fhirServerBase, proxyUrl, proxyUsername,
				proxyPassword, hapiClientVerbose, fhirContext, localIdentifierValue, dataLogger);
	}

	protected AsyncFhirClient createAsyncFhirClient()
	{
		KeyStore trustStore = readTrustStore();

		char[] keyStorePassword = UUID.randomUUID().toString().toCharArray();
		KeyStore keyStore = readKeyStore(keyStorePassword);

		return new AsyncFhirClientImpl(trustStore, keyStore, keyStorePassword, connectTimeout, socketTimeout,
				fhirServerBasicAuthUsername, fhirServerBasicAuthPassword, fhirServerBearerToken,
				fhirServerOAuth2TokenProvider, fhirServerBase, proxyUrl, proxyUsername, proxyPassword,
				initialPollingIntervalMilliseconds, fhirContext, localIdentifierValue, dataLogger);
	}

	protected BinaryStreamFhirClient createBinaryStreamClient()
	{
		KeyStore trustStore = readTrustStore();

		char[] keyStorePassword = UUID.randomUUID().toString().toCharArray();
		KeyStore keyStore = readKeyStore(keyStorePassword);

		return new BinaryStreamFhirClientImpl(trustStore, keyStore, keyStorePassword, connectTimeout, socketTimeout,
				fhirServerBasicAuthUsername, fhirServerBasicAuthPassword, fhirServerBearerToken,
				fhirServerOAuth2TokenProvider, fhirServerBase, proxyUrl, proxyUsername, proxyPassword, fhirContext,
				localIdentifierValue, dataLogger);
	}

	private KeyStore readTrustStore()
	{
		if (trustStorePath == null)
			return null;

		try
		{
			logger.debug("Creating truststore from {}", trustStorePath.toString());
			return CertificateReader.allFromCer(trustStorePath);
		}
		catch (NoSuchAlgorithmException | CertificateException | KeyStoreException | IOException e)
		{
			throw new RuntimeException(e);
		}

	}

	private KeyStore readKeyStore(char[] keyStorePassword)
	{
		if (certificatePath == null || privateKeyPath == null || privateKeyPassword == null)
			return null;

		try
		{
			logger.debug("Creating client keystore from {} and {} with password {}", certificatePath.toString(),
					privateKeyPath.toString(), "***");

			PrivateKey privateKey = PemIo.readPrivateKeyFromPem(privateKeyPath, privateKeyPassword);
			X509Certificate certificate = PemIo.readX509CertificateFromPem(certificatePath);

			return CertificateHelper.toJksKeyStore(privateKey, new Certificate[] { certificate },
					UUID.randomUUID().toString(), keyStorePassword);
		}
		catch (NoSuchAlgorithmException | CertificateException | KeyStoreException | IOException
				| PKCSException exception)
		{
			throw new RuntimeException(exception);
		}
	}

	public String getFhirBaseUrl()
	{
		return fhirServerBase;
	}
}
