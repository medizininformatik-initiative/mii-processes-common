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
import java.util.UUID;

import org.bouncycastle.pkcs.PKCSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import de.hsheilbronn.mi.utils.crypto.CertificateHelper;
import de.hsheilbronn.mi.utils.crypto.io.CertificateReader;
import de.hsheilbronn.mi.utils.crypto.io.PemIo;
import de.medizininformatik_initiative.processes.common.fhir.client.logging.DataLogger;

public class FhirClientFactory
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

	private final String proxyUrl;
	private final String proxyUsername;
	private final String proxyPassword;

	private final boolean hapiClientVerbose;

	private final FhirContext fhirContext;

	private final String localIdentifierValue;

	private final DataLogger dataLogger;

	public FhirClientFactory(Path trustStorePath, Path certificatePath, Path privateKeyPath, char[] privateKeyPassword,
			int connectTimeout, int socketTimeout, int connectionRequestTimeout, String fhirServerBase,
			String fhirServerBasicAuthUsername, String fhirServerBasicAuthPassword, String fhirServerBearerToken,
			String proxyUrl, String proxyUsername, String proxyPassword, boolean hapiClientVerbose,
			FhirContext fhirContext, String localIdentifierValue, DataLogger dataLogger)
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

		this.proxyUrl = proxyUrl;
		this.proxyUsername = proxyUsername;
		this.proxyPassword = proxyPassword;
		this.hapiClientVerbose = hapiClientVerbose;

		this.fhirContext = fhirContext;

		this.localIdentifierValue = localIdentifierValue;

		this.dataLogger = dataLogger;
	}

	public void testConnection()
	{
		try
		{
			logger.info(
					"Testing connection to FHIR server with {trustStorePath: {}, certificatePath: {}, privateKeyPath: {}, privateKeyPassword: {},"
							+ " basicAuthUsername {}, basicAuthPassword {}, bearerToken {}, serverBase: {}, proxyUrl {}, proxyUsername {}, proxyPassword {}}",
					trustStorePath, certificatePath, privateKeyPath, privateKeyPassword != null ? "***" : "null",
					fhirServerBasicAuthUsername, fhirServerBasicAuthPassword != null ? "***" : "null",
					fhirServerBearerToken != null ? "***" : "null", fhirServerBase, proxyUrl, proxyUsername,
					proxyPassword != null ? "***" : "null");

			getFhirClient().testConnection();
		}
		catch (Exception e)
		{
			logger.error("Error while testing connection to FHIR server", e);
		}
	}

	public FhirClient getFhirClient()
	{
		if (configured())
			return createFhirClientImpl();
		else
			throw new RuntimeException("Configuration error: FHIR server base url not set");
	}

	private boolean configured()
	{
		return fhirServerBase != null && !fhirServerBase.isBlank();
	}

	protected FhirClient createFhirClientImpl()
	{
		KeyStore trustStore = null;
		char[] keyStorePassword = null;
		if (trustStorePath != null)
		{
			logger.debug("Reading trust-store from {}", trustStorePath.toString());
			trustStore = readTrustStore(trustStorePath);
			keyStorePassword = UUID.randomUUID().toString().toCharArray();
		}

		KeyStore keyStore = null;
		if (certificatePath != null && privateKeyPath != null)
		{
			logger.debug("Creating key-store from {} and {} with password {}", certificatePath.toString(),
					privateKeyPath.toString(), keyStorePassword != null ? "***" : "null");
			keyStore = readKeyStore(certificatePath, privateKeyPath, privateKeyPassword, keyStorePassword);
		}

		return new FhirClientImpl(trustStore, keyStore, keyStorePassword, connectTimeout, socketTimeout,
				connectionRequestTimeout, fhirServerBasicAuthUsername, fhirServerBasicAuthPassword,
				fhirServerBearerToken, fhirServerBase, proxyUrl, proxyUsername, proxyPassword, hapiClientVerbose,
				fhirContext, localIdentifierValue, dataLogger);
	}

	private KeyStore readTrustStore(Path trustPath)
	{
		try
		{
			return CertificateReader.allFromCer(trustPath);
		}
		catch (NoSuchAlgorithmException | CertificateException | KeyStoreException | IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	private KeyStore readKeyStore(Path certificatePath, Path keyPath, char[] keyPassword, char[] keyStorePassword)
	{
		try
		{
			PrivateKey privateKey = PemIo.readPrivateKeyFromPem(keyPath, keyPassword);
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
}
