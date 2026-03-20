package de.medizininformatik_initiative.processes.common.crypto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Optional;

import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.hsheilbronn.mi.utils.crypto.io.PemReader;
import dev.dsf.bpe.v2.ProcessPluginApi;

public interface KeyProvider
{
	Logger logger = LoggerFactory.getLogger(KeyProvider.class);

	/**
	 * @return can be <code>null</code>
	 */
	PrivateKey getPrivateKey();

	/**
	 * @return can be <code>null</code>
	 */
	PublicKey getPublicKey();

	/**
	 * Creates a PublicKey based on a {@link org.hl7.fhir.r4.model.Bundle} with type
	 * {@link org.hl7.fhir.r4.model.Bundle.BundleType#COLLECTION} containing a
	 * {@link org.hl7.fhir.r4.model.DocumentReference} with an {@link org.hl7.fhir.r4.model.Identifier} matching system
	 * {@link de.medizininformatik_initiative.processes.common.util.ConstantsBase#CODESYSTEM_MII_CRYPTOGRAPHY} and code
	 * {@link de.medizininformatik_initiative.processes.common.util.ConstantsBase#CODESYSTEM_MII_CRYPTOGRAPHY_VALUE_PUBLIC_KEY}
	 * and a {@link org.hl7.fhir.r4.model.Binary} attachment based on a PublicKey provided by {@link #getPublicKey()} on
	 * the local DSF FHIR server.
	 */
	void createPublicKeyIfNotExists();

	/**
	 * Reads a PublicKey based on a {@link org.hl7.fhir.r4.model.Bundle} with type
	 * {@link org.hl7.fhir.r4.model.Bundle.BundleType#COLLECTION} containing a
	 * {@link org.hl7.fhir.r4.model.DocumentReference} with an {@link org.hl7.fhir.r4.model.Identifier} matching system
	 * {@link de.medizininformatik_initiative.processes.common.util.ConstantsBase#CODESYSTEM_MII_CRYPTOGRAPHY} and code
	 * {@link de.medizininformatik_initiative.processes.common.util.ConstantsBase#CODESYSTEM_MII_CRYPTOGRAPHY_VALUE_PUBLIC_KEY}
	 * and a {@link org.hl7.fhir.r4.model.Binary} attachment based on a PublicKey provided by {@link #getPublicKey()} on
	 * a local or remote DSF FHIR server.
	 *
	 * @param baseUrl
	 *            the base URL used to connect to the local or remote DSF FHIR server, not <code>null</code> or empty
	 * @return {@link Optional<org.hl7.fhir.r4.model.Bundle>} if a PublicKey exists, {@link Optional#empty()} otherwise
	 */
	Optional<Bundle> readPublicKeyIfExists(String baseUrl);

	// openssl genrsa -out keypair.pem 4096
	// openssl rsa -in keypair.pem -pubout -out publickey.crt
	// openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in keypair.pem -out pkcs8.key

	/**
	 * Creating a KeyProvider where private and public key are <code>null</code>.
	 *
	 * @param api
	 *            not <code>null</code>
	 * @return KeyProvider
	 */
	static KeyProvider from(ProcessPluginApi api)
	{
		return new KeyProviderImpl(api, null, null);
	}

	/**
	 * Creating a KeyProvider based on private and public key files in PEM format. The keys must be RSA keys and must
	 * match each other.
	 *
	 * @param api
	 *            not <code>null</code>
	 * @param privateKeyFile
	 *            not <code>null</code>
	 * @param publicKeyFile
	 *            not <code>null</code>
	 * @return KeyProvider
	 */
	static KeyProvider from(ProcessPluginApi api, String privateKeyFile, String publicKeyFile)
	{
		logger.info("Configuring KeyProvider with private-key from '{}' and public-key from '{}'", privateKeyFile,
				publicKeyFile);

		PrivateKey privateKey = null;
		PublicKey publicKey = null;

		try
		{
			if (privateKeyFile != null)
			{
				Path privateKeyPath = Paths.get(privateKeyFile);
				if (!Files.isReadable(privateKeyPath))
					throw new RuntimeException("PrivateKey at '" + privateKeyFile + "' not readable");

				privateKey = PemReader.readPrivateKey(privateKeyPath);

				if (!(privateKey instanceof RSAPrivateKey))
				{
					throw new IllegalArgumentException(
							"PrivateKey '%s' is not an RSA based private key. Only RSA is supported."
									.formatted(privateKeyFile));
				}
			}
		}
		catch (IOException e)
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

				publicKey = PemReaderPublicKey.readPublicKey(publicKeyPath);

				if (!(publicKey instanceof RSAPublicKey))
				{
					throw new IllegalArgumentException(
							"PublicKey '%s' is not an RSA based public key. Only RSA is supported."
									.formatted(privateKeyFile));
				}
			}
		}
		catch (IOException e)
		{
			throw new RuntimeException("Error while reading PublicKey from '" + publicKeyFile + "'", e);
		}

		if (privateKey != null && publicKey != null)
		{
			if (!((RSAPrivateKey) privateKey).getModulus().equals(((RSAPublicKey) publicKey).getModulus())
					|| ((privateKey instanceof RSAPrivateCrtKey) && !((RSAPrivateCrtKey) privateKey).getPublicExponent()
							.equals(((RSAPublicKey) publicKey).getPublicExponent())))
			{
				throw new IllegalArgumentException(
						"PrivateKey '%s' and PublicKey '%s' do not match.".formatted(privateKeyFile, publicKeyFile));
			}
		}

		return new KeyProviderImpl(api, privateKey, publicKey);
	}

	/**
	 * @param bytes
	 *            containing the PublicKey data, not <code>null</code>
	 * @return not <code>null</code>
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException
	 */
	static PublicKey from(byte[] bytes) throws NoSuchAlgorithmException, InvalidKeySpecException
	{
		PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes));

		if (publicKey instanceof RSAPublicKey)
		{
			return publicKey;
		}
		else
		{
			throw new IllegalStateException("Provided bytes are not an RSA based public key. Only RSA is supported.");
		}
	}
}
