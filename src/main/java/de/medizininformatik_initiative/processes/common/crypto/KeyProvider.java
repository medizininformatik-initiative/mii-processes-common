package de.medizininformatik_initiative.processes.common.crypto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.XECPrivateKey;
import java.security.interfaces.XECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Optional;

import javax.crypto.KeyAgreement;

import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.hsheilbronn.mi.utils.crypto.io.PemReader;
import dev.dsf.bpe.v2.ProcessPluginApi;

public interface KeyProvider
{
	Logger logger = LoggerFactory.getLogger(KeyProvider.class);

	String ALGORITHM_X25519 = "X25519";

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
	 * {@link de.medizininformatik_initiative.processes.common.util.ConstantsBase#NAMINGSYSTEM_MII_RECEIVER_KEY_ID} and
	 * value based on provided {@param receiverKeyId}, and a {@link org.hl7.fhir.r4.model.Binary} attachment containing
	 * a PublicKey provided by {@link #getPublicKey()} on the local DSF FHIR server.
	 *
	 * @param receiverKeyId
	 *            the receiverKeyId used as {@link org.hl7.fhir.r4.model.Identifier}.value for the NamingSystem
	 *            {@link de.medizininformatik_initiative.processes.common.util.ConstantsBase#NAMINGSYSTEM_MII_RECEIVER_KEY_ID}
	 */
	void createPublicKeyIfNotExists(String receiverKeyId);

	/**
	 * Reads a PublicKey based on a {@link org.hl7.fhir.r4.model.Bundle} with type
	 * {@link org.hl7.fhir.r4.model.Bundle.BundleType#COLLECTION} containing a
	 * {@link org.hl7.fhir.r4.model.DocumentReference} with an {@link org.hl7.fhir.r4.model.Identifier} matching system
	 * {@link de.medizininformatik_initiative.processes.common.util.ConstantsBase#NAMINGSYSTEM_MII_RECEIVER_KEY_ID} and
	 * value based on provided {@param receiverKeyId}, and a {@link org.hl7.fhir.r4.model.Binary} attachment containing
	 * a PublicKey provided by {@link #getPublicKey()} on a local or remote DSF FHIR server.
	 *
	 * @param receiverKeyId
	 *            the receiverKeyId used as {@link org.hl7.fhir.r4.model.Identifier}.value for the NamingSystem
	 *            {@link de.medizininformatik_initiative.processes.common.util.ConstantsBase#NAMINGSYSTEM_MII_RECEIVER_KEY_ID}
	 * @return {@link Optional<org.hl7.fhir.r4.model.Bundle>} if a PublicKey exists, {@link Optional#empty()} otherwise
	 */
	Optional<Bundle> readPublicKeyIfExists(String receiverKeyId, String baseUrl);

	/**
	 * Deletes a PublicKey based on a {@link org.hl7.fhir.r4.model.Bundle} with type
	 * {@link org.hl7.fhir.r4.model.Bundle.BundleType#COLLECTION} containing a
	 * {@link org.hl7.fhir.r4.model.DocumentReference} with an {@link org.hl7.fhir.r4.model.Identifier} matching system
	 * {@link de.medizininformatik_initiative.processes.common.util.ConstantsBase#NAMINGSYSTEM_MII_RECEIVER_KEY_ID} and
	 * value based on provided {@param receiverKeyId}, and a {@link org.hl7.fhir.r4.model.Binary} attachment containing
	 * a PublicKey provided by {@link #getPublicKey()} on the local DSF FHIR server.
	 *
	 * @param receiverKeyId
	 *            the receiverKeyId used as {@link org.hl7.fhir.r4.model.Identifier}.value for the NamingSystem
	 *            {@link de.medizininformatik_initiative.processes.common.util.ConstantsBase#NAMINGSYSTEM_MII_RECEIVER_KEY_ID}
	 */
	public void deletePublicKeyIfExists(String receiverKeyId);

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
	static KeyProvider forX25519From(ProcessPluginApi api)
	{
		return new KeyProviderX25519(api, null, null);
	}

	/**
	 * Creating a KeyProvider based on private and public key files in PEM format. The keys must be x25519 EC keys and
	 * must match each other.
	 *
	 * @param api
	 *            not <code>null</code>
	 * @param privateKeyFile
	 *            not <code>null</code>
	 * @param publicKeyFile
	 *            not <code>null</code>
	 * @return KeyProvider
	 */
	static KeyProvider forX25519From(ProcessPluginApi api, String privateKeyFile, String publicKeyFile)
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

				if (!(privateKey instanceof XECPrivateKey)
						&& !(ALGORITHM_X25519.equalsIgnoreCase(privateKey.getAlgorithm())))
				{
					throw new IllegalArgumentException(
							"PrivateKey '%s' is not an x25519 EC based private key. Only x25519 EC is supported."
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

				if (!(publicKey instanceof XECPublicKey)
						&& !(ALGORITHM_X25519.equalsIgnoreCase(publicKey.getAlgorithm())))
				{
					throw new IllegalArgumentException(
							"PublicKey '%s' is not an x25519 EC based public key. Only x25519 EC is supported."
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
			if (!x25519KeysBelongTogether(privateKey, publicKey))
			{
				throw new IllegalArgumentException(
						"PrivateKey '%s' and PublicKey '%s' do not match.".formatted(privateKeyFile, publicKeyFile));
			}
		}

		return new KeyProviderX25519(api, privateKey, publicKey);
	}

	private static boolean x25519KeysBelongTogether(PrivateKey privateKey, PublicKey publicKey)
	{
		try
		{
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM_X25519);
			KeyPair probeKeyPair = keyPairGenerator.generateKeyPair();

			byte[] secret1 = x25519Agreement(privateKey, probeKeyPair.getPublic());
			byte[] secret2 = x25519Agreement(probeKeyPair.getPrivate(), publicKey);

			return MessageDigest.isEqual(secret1, secret2);
		}
		catch (Exception exception)
		{
			throw new IllegalArgumentException("Could not verify if x25519 private/public key match.", exception);
		}
	}

	private static byte[] x25519Agreement(PrivateKey privateKey, PublicKey publicKey) throws Exception
	{
		KeyAgreement keyAgreement = KeyAgreement.getInstance(ALGORITHM_X25519);
		keyAgreement.init(privateKey);
		keyAgreement.doPhase(publicKey, true);
		return keyAgreement.generateSecret();
	}

	/**
	 * @param bytes
	 *            containing the PublicKey data, not <code>null</code>
	 * @return not <code>null</code>
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException
	 */
	static PublicKey forX25519From(byte[] bytes) throws NoSuchAlgorithmException, InvalidKeySpecException
	{
		PublicKey publicKey = KeyFactory.getInstance(ALGORITHM_X25519).generatePublic(new X509EncodedKeySpec(bytes));

		if (!(publicKey instanceof XECPublicKey) && !(ALGORITHM_X25519.equalsIgnoreCase(publicKey.getAlgorithm())))
		{
			throw new IllegalStateException(
					"Provided bytes are not an x25519 EC based public key. Only x25519 EC is supported.");
		}

		return publicKey;
	}
}
