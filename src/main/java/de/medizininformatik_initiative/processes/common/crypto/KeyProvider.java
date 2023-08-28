package de.medizininformatik_initiative.processes.common.crypto;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Optional;

import org.hl7.fhir.r4.model.Bundle;

public interface KeyProvider
{
	/**
	 * @return not <code>null</code>
	 */
	PrivateKey getPrivateKey();

	/**
	 * @return not <code>null</code>
	 */
	PublicKey getPublicKey();

	/**
	 * Creating a PublicKey based on a {@link org.hl7.fhir.r4.model.Bundle} with type
	 * {@link org.hl7.fhir.r4.model.Bundle.BundleType#COLLECTION} containing a
	 * {@link org.hl7.fhir.r4.model.DocumentReference} with an {@link org.hl7.fhir.r4.model.Identifier} matching system
	 * {@link de.medizininformatik_initiative.processes.common.util.ConstantsBase#CODESYSTEM_MII_CRYPTOGRAPHY} and code
	 * {@link de.medizininformatik_initiative.processes.common.util.ConstantsBase#CODESYSTEM_MII_CRYPTOGRAPHY_VALUE_PUBLIC_KEY}
	 * and a {@link org.hl7.fhir.r4.model.Binary} attachment based on a PublicKey provided by {@link #getPublicKey()} on
	 * the local DSF FHIR server.
	 */
	void createPublicKeyIfNotExists();

	/**
	 * Reading a PublicKey based on a {@link org.hl7.fhir.r4.model.Bundle} with type
	 * {@link org.hl7.fhir.r4.model.Bundle.BundleType#COLLECTION} containing a
	 * {@link org.hl7.fhir.r4.model.DocumentReference} with an {@link org.hl7.fhir.r4.model.Identifier} matching system
	 * {@link de.medizininformatik_initiative.processes.common.util.ConstantsBase#CODESYSTEM_MII_CRYPTOGRAPHY} and code
	 * {@link de.medizininformatik_initiative.processes.common.util.ConstantsBase#CODESYSTEM_MII_CRYPTOGRAPHY_VALUE_PUBLIC_KEY}
	 * and a {@link org.hl7.fhir.r4.model.Binary} attachment based on a PublicKey provided by {@link #getPublicKey()} on
	 * a local or remote DSF FHIR server.
	 *
	 * @param webserviceUrl
	 *            the base Url used to connect to the local or remote DSF FHIR server, note <code>null</code> or empty
	 * @return {@link Optional<org.hl7.fhir.r4.model.Bundle>} if a PublicKey exists, {@link Optional#empty()} otherwise
	 */
	Optional<Bundle> readPublicKeyIfExists(String webserviceUrl);

	/**
	 * @param bytes
	 *            containing the PublicKey data, not <code>null</code>
	 * @return not <code>null</code>
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException
	 */
	static PublicKey fromBytes(byte[] bytes) throws NoSuchAlgorithmException, InvalidKeySpecException
	{
		PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes));

		if (publicKey instanceof RSAPublicKey)
		{
			return publicKey;
		}
		else
		{
			throw new IllegalStateException("PublicKey not a RSAPublicKey");
		}
	}
}
