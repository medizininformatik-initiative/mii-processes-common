package de.medizininformatik_initiative.processes.common.crypto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import org.junit.Test;

import de.hsheilbronn.mi.utils.crypto.io.PemWriter;

public class KeyProviderImplTest
{
	@Test
	public void errorOnPrivateAndPublicKeyFilesAreNull() throws Exception
	{
		Exception exception = assertThrows(NullPointerException.class,
				() -> KeyProviderImpl.fromFiles(null, null, null, null));

		assertEquals("privateKeyFile path must not be null", exception.getMessage());
	}

	@Test
	public void errorOnPrivateKeyFileIsNull() throws Exception
	{
		var generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(1024);
		var nonMatchingPublicKey = (RSAPublicKey) generator.generateKeyPair().getPublic();
		var publicKeyFile = File.createTempFile("publicKey", ".pem");
		publicKeyFile.deleteOnExit();
		PemWriter.writePublicKey(nonMatchingPublicKey, publicKeyFile.toPath());

		Exception exception = assertThrows(NullPointerException.class,
				() -> KeyProviderImpl.fromFiles(null, null, publicKeyFile.getAbsolutePath(), null));

		assertEquals("privateKeyFile path must not be null", exception.getMessage());
	}

	@Test
	public void errorOnPublicKeyFileIsNull() throws Exception
	{
		var generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(1024);
		var privateKey = (RSAPrivateKey) generator.generateKeyPair().getPrivate();
		var privateKeyFile = File.createTempFile("privateKey", ".pem");
		privateKeyFile.deleteOnExit();
		PemWriter.writePrivateKey(privateKey).asOpenSslClassic().notEncrypted().toFile(privateKeyFile.toPath());

		Exception exception = assertThrows(NullPointerException.class,
				() -> KeyProviderImpl.fromFiles(null, privateKeyFile.getAbsolutePath(), null, null));

		assertEquals("publicKeyFile path must not be null", exception.getMessage());
	}

	@Test
	public void errorOnNonMatchingPrivateAndPublicKey() throws Exception
	{
		var generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(1024);
		var privateKey = (RSAPrivateKey) generator.generateKeyPair().getPrivate();
		var nonMatchingPublicKey = (RSAPublicKey) generator.generateKeyPair().getPublic();
		var privateKeyFile = File.createTempFile("privateKey", ".pem");
		var publicKeyFile = File.createTempFile("publicKey", ".pem");
		privateKeyFile.deleteOnExit();
		publicKeyFile.deleteOnExit();
		PemWriter.writePublicKey(nonMatchingPublicKey, publicKeyFile.toPath());
		PemWriter.writePrivateKey(privateKey).asOpenSslClassic().notEncrypted().toFile(privateKeyFile.toPath());

		assertNotEquals(privateKey.getModulus(), nonMatchingPublicKey.getModulus());

		Exception exception = assertThrows(IllegalArgumentException.class, () -> KeyProviderImpl.fromFiles(null,
				privateKeyFile.getAbsolutePath(), publicKeyFile.getAbsolutePath(), null));

		assertEquals("PrivateKey '%s' and PublicKey '%s' do not match.".formatted(privateKeyFile.getAbsolutePath(),
				publicKeyFile.getAbsolutePath()), exception.getMessage());
	}

	@Test
	public void errorOnNonRSAPrivateKey() throws Exception
	{
		var dsaGenerator = KeyPairGenerator.getInstance("DSA");
		var rsaGenerator = KeyPairGenerator.getInstance("RSA");
		dsaGenerator.initialize(512);
		rsaGenerator.initialize(1024);
		var privateKey = dsaGenerator.generateKeyPair().getPrivate();
		var nonMatchingPublicKey = (RSAPublicKey) rsaGenerator.generateKeyPair().getPublic();
		var privateKeyFile = File.createTempFile("privateKey", ".pem");
		var publicKeyFile = File.createTempFile("publicKey", ".pem");
		privateKeyFile.deleteOnExit();
		publicKeyFile.deleteOnExit();
		PemWriter.writePublicKey(nonMatchingPublicKey, publicKeyFile.toPath());
		PemWriter.writePrivateKey(privateKey).asOpenSslClassic().notEncrypted().toFile(privateKeyFile.toPath());

		assertFalse(privateKey instanceof RSAPrivateKey);

		Exception exception = assertThrows(IllegalArgumentException.class, () -> KeyProviderImpl.fromFiles(null,
				privateKeyFile.getAbsolutePath(), publicKeyFile.getAbsolutePath(), null));

		assertTrue(exception.getMessage().contains("is not an RSA based private key. Only RSA is supported."));
	}

	@Test
	public void matchingPrivateAndPublicKey() throws Exception
	{
		var generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(1024);
		var keyPair = generator.generateKeyPair();
		var privateKey = (RSAPrivateKey) keyPair.getPrivate();
		var matchingPublicKey = (RSAPublicKey) keyPair.getPublic();
		var privateKeyFile = File.createTempFile("privateKey", ".pem");
		var publicKeyFile = File.createTempFile("publicKey", ".pem");
		privateKeyFile.deleteOnExit();
		publicKeyFile.deleteOnExit();
		PemWriter.writePublicKey(matchingPublicKey, publicKeyFile.toPath());
		PemWriter.writePrivateKey(privateKey).asOpenSslClassic().notEncrypted().toFile(privateKeyFile.toPath());

		var provider = KeyProviderImpl.fromFiles(null, privateKeyFile.getAbsolutePath(),
				publicKeyFile.getAbsolutePath(), null);

		assertEquals(privateKey.getModulus(), ((RSAPrivateKey) provider.getPrivateKey()).getModulus());

		assertEquals(matchingPublicKey.getModulus(), ((RSAPublicKey) provider.getPublicKey()).getModulus());

		assertEquals(((RSAPrivateKey) provider.getPrivateKey()).getModulus(),
				((RSAPublicKey) provider.getPublicKey()).getModulus());
	}
}
