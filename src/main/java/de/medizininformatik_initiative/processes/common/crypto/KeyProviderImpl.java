package de.medizininformatik_initiative.processes.common.crypto;

import static org.hl7.fhir.r4.model.Bundle.BundleType.COLLECTION;
import static org.hl7.fhir.r4.model.DocumentReference.ReferredDocumentStatus.FINAL;
import static org.hl7.fhir.r4.model.Enumerations.DocumentReferenceStatus.CURRENT;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.bouncycastle.pkcs.PKCSException;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import de.medizininformatik_initiative.processes.common.fhir.client.logging.DataLogger;
import de.medizininformatik_initiative.processes.common.util.ConstantsBase;
import de.rwh.utils.crypto.io.PemIo;
import dev.dsf.bpe.v1.ProcessPluginApi;

public class KeyProviderImpl implements KeyProvider, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(KeyProviderImpl.class);

	// openssl genrsa -out keypair.pem 4096
	// openssl rsa -in keypair.pem -pubout -out publickey.crt
	// openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in keypair.pem -out pkcs8.key

	/**
	 * One or both parameters should be <code>null</code>
	 *
	 * @param api
	 *            not <code>null</code>
	 * @param privateKeyFile
	 *            not <code>null</code>
	 * @param publicKeyFile
	 *            not <code>null</code>
	 * @return KeyProvider
	 */
	public static KeyProviderImpl fromFiles(ProcessPluginApi api, String privateKeyFile, String publicKeyFile,
			DataLogger dataLogger)
	{
		logger.info("Configuring KeyProvider with private-key from '{}' and public-key from '{}'", privateKeyFile,
				publicKeyFile);

		PrivateKey privateKey = null;
		RSAPublicKey publicKey = null;

		try
		{
			if (privateKeyFile != null)
			{
				Path privateKeyPath = Paths.get(privateKeyFile);
				if (!Files.isReadable(privateKeyPath))
					throw new RuntimeException("PrivateKey at '" + privateKeyFile + "' not readable");

				privateKey = PemIo.readPrivateKeyFromPem(privateKeyPath);
			}
		}
		catch (IOException | PKCSException e)
		{
			throw new RuntimeException("Error while reading PrivateKey from '" + privateKeyFile + "'", e);
		}

		try
		{
			if (publicKeyFile != null)
			{
				Path publicKeyPath = Paths.get(publicKeyFile);
				if (!Files.isReadable(publicKeyPath))
					throw new RuntimeException("PublicKey at '" + publicKeyFile + "' not readable");

				publicKey = PemIo.readPublicKeyFromPem(publicKeyPath);
			}
		}
		catch (NoSuchAlgorithmException | InvalidKeySpecException | IOException e)
		{
			throw new RuntimeException("Error while reading PublicKey from '" + publicKeyFile + "'", e);
		}

		return new KeyProviderImpl(api, privateKey, publicKey, dataLogger);
	}

	private final PrivateKey privateKey;
	private final PublicKey publicKey;

	private final ProcessPluginApi api;
	private final DataLogger dataLogger;

	public KeyProviderImpl(ProcessPluginApi api, PrivateKey privateKey, PublicKey publicKey, DataLogger dataLogger)
	{
		this.api = api;
		this.privateKey = privateKey;
		this.publicKey = publicKey;
		this.dataLogger = dataLogger;
	}

	@Override
	public void afterPropertiesSet()
	{
		Objects.requireNonNull(api, "api");
		Objects.requireNonNull(dataLogger, "dataLogger");
	}

	public void createPublicKeyIfNotExists()
	{
		try
		{
			if (publicKey != null)
			{
				String baseUrl = api.getEndpointProvider().getLocalEndpointAddress();
				Optional<Bundle> bundleOnServer = readPublicKeyIfExists(baseUrl);

				if (bundleOnServer.isPresent())
				{
					logger.info("PublicKey Bundle already exists on DSF FHIR server with base Url '{}'", baseUrl);
				}
				else
				{
					logger.info("Creating new PublicKey Bundle on DSF FHIR server with baseUrl '{}' ...", baseUrl);
					Bundle bundleToCreate = createPublicKeyBundle();
					bundleOnServer = Optional
							.of(api.getFhirWebserviceClientProvider().getLocalWebserviceClient().createConditionaly(
									bundleToCreate, "identifier=" + ConstantsBase.CODESYSTEM_MII_CRYPTOGRAPHY + "|"
											+ ConstantsBase.CODESYSTEM_MII_CRYPTOGRAPHY_VALUE_PUBLIC_KEY));
				}

				IdType bundleOnServerId = bundleOnServer.get().getIdElement();
				bundleOnServerId.setIdBase(baseUrl);
				logger.info("PublicKey Bundle has id '{}'", bundleOnServerId.getValue());
			}
		}
		catch (Exception exception)
		{
			throw new RuntimeException("Error while creating PublicKey Bundle: " + exception.getMessage(), exception);
		}
	}

	@Override
	public Optional<Bundle> readPublicKeyIfExists(String webserviceUrl)
	{
		logger.info("Reading PublicKey Bundle on DSF FHIR server with baseUrl '{}' ...", webserviceUrl);

		Bundle publicKeyBundle = api.getFhirWebserviceClientProvider().getWebserviceClient(webserviceUrl).search(
				Bundle.class, Map.of("identifier", Collections.singletonList(ConstantsBase.CODESYSTEM_MII_CRYPTOGRAPHY
						+ "|" + ConstantsBase.CODESYSTEM_MII_CRYPTOGRAPHY_VALUE_PUBLIC_KEY)));

		int total = publicKeyBundle.getTotal();

		if (total >= 1)
		{
			if (total > 1)
				logger.warn(
						"PublicKey Bundle on DSF FHIR server with baseUrl '{}' contains > 1 entries ({}), using the first",
						webserviceUrl, total);

			return Optional.of((Bundle) publicKeyBundle.getEntryFirstRep().getResource());
		}
		else
		{
			logger.debug("PublicKey Bundle on DSF FHIR server with baseUrl '{}' is empty", webserviceUrl);
			return Optional.empty();
		}
	}

	private Bundle createPublicKeyBundle()
	{
		Date date = new Date();

		Binary binary = new Binary().setContentType("application/pem-certificate-chain");
		binary.setContent(getPublicKey().getEncoded());
		binary.setId(UUID.randomUUID().toString());

		DocumentReference documentReference = new DocumentReference().setStatus(CURRENT).setDocStatus(FINAL);
		documentReference.getMasterIdentifier().setSystem(ConstantsBase.CODESYSTEM_MII_CRYPTOGRAPHY)
				.setValue(ConstantsBase.CODESYSTEM_MII_CRYPTOGRAPHY_VALUE_PUBLIC_KEY);
		documentReference.addAuthor().setType(ResourceType.Organization.name())
				.setIdentifier(api.getOrganizationProvider().getLocalOrganizationIdentifier().get());
		documentReference.setDate(date);
		documentReference.addContent().getAttachment().setContentType("application/pem-certificate-chain")
				.setUrl("urn:uuid:" + binary.getId()).setHash(DigestUtils.sha256(publicKey.getEncoded()));

		Bundle bundle = new Bundle().setType(COLLECTION);
		bundle.getIdentifier().setSystem(ConstantsBase.CODESYSTEM_MII_CRYPTOGRAPHY)
				.setValue(ConstantsBase.CODESYSTEM_MII_CRYPTOGRAPHY_VALUE_PUBLIC_KEY);
		bundle.setTimestamp(date);
		bundle.addEntry().setResource(documentReference).setFullUrl("urn:uuid:" + documentReference.getId());
		bundle.addEntry().setResource(binary).setFullUrl("urn:uuid:" + binary.getId());

		api.getReadAccessHelper().addAll(bundle);

		dataLogger.logResource("Created PublicKey Bundle", bundle);

		return bundle;
	}

	@Override
	public PrivateKey getPrivateKey()
	{
		return privateKey;
	}

	@Override
	public PublicKey getPublicKey()
	{
		return publicKey;
	}
}
