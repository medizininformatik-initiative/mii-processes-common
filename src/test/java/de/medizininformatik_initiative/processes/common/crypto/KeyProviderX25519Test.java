package de.medizininformatik_initiative.processes.common.crypto;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.security.KeyPair;
import java.util.Base64;

import org.junit.Test;

import de.hsheilbronn.mi.utils.crypto.keypair.KeyPairGeneratorFactory;

public class KeyProviderX25519Test
{
	@Test
	public void errorOnNonX25519PrivateKey() throws Exception
	{
		var nonX25519KeyPair = KeyPairGeneratorFactory.rsa(4096).initialize().generateKeyPair();
		var x25519KeyPair = KeyPairGeneratorFactory.x25519().initialize().generateKeyPair();

		var privateKeyFile = File.createTempFile("privateKey", ".pem");
		var publicKeyFile = File.createTempFile("publicKey", ".pem");
		privateKeyFile.deleteOnExit();
		publicKeyFile.deleteOnExit();

		writePrivateKey(nonX25519KeyPair, privateKeyFile);
		writePublicKey(x25519KeyPair, publicKeyFile);

		Exception exception = assertThrows(IllegalArgumentException.class, () -> KeyProvider.forX25519From(null,
				privateKeyFile.getAbsolutePath(), publicKeyFile.getAbsolutePath()));

		assertTrue(exception.getMessage().contains("is not an x25519 EC based private key"));
	}

	@Test
	public void errorOnNonX25519PublicKey() throws Exception
	{
		var nonX25519KeyPair = KeyPairGeneratorFactory.rsa(4096).initialize().generateKeyPair();
		var x25519KeyPair = KeyPairGeneratorFactory.x25519().initialize().generateKeyPair();

		var privateKeyFile = File.createTempFile("privateKey", ".pem");
		var publicKeyFile = File.createTempFile("publicKey", ".pem");
		privateKeyFile.deleteOnExit();
		publicKeyFile.deleteOnExit();

		writePrivateKey(x25519KeyPair, privateKeyFile);
		writePublicKey(nonX25519KeyPair, publicKeyFile);

		Exception exception = assertThrows(IllegalArgumentException.class, () -> KeyProvider.forX25519From(null,
				privateKeyFile.getAbsolutePath(), publicKeyFile.getAbsolutePath()));

		assertTrue(exception.getMessage().contains("is not an x25519 EC based public key"));
	}

	@Test
	public void errorOnNonMatchingPrivateAndPublicKey() throws Exception
	{
		var keyPair = KeyPairGeneratorFactory.x25519().initialize().generateKeyPair();
		var nonMatchingKeyPair = KeyPairGeneratorFactory.x25519().initialize().generateKeyPair();

		var privateKeyFile = File.createTempFile("privateKey", ".pem");
		var publicKeyFile = File.createTempFile("publicKey", ".pem");
		privateKeyFile.deleteOnExit();
		publicKeyFile.deleteOnExit();

		writePrivateKey(keyPair, privateKeyFile);
		writePublicKey(nonMatchingKeyPair, publicKeyFile);

		Exception exception = assertThrows(IllegalArgumentException.class, () -> KeyProvider.forX25519From(null,
				privateKeyFile.getAbsolutePath(), publicKeyFile.getAbsolutePath()));

		assertTrue(exception.getMessage().contains("do not match"));
	}

	@Test
	public void matchingPrivateAndPublicKey() throws Exception
	{
		var keyPair = KeyPairGeneratorFactory.x25519().initialize().generateKeyPair();

		var privateKeyFile = File.createTempFile("privateKey", ".pem");
		var publicKeyFile = File.createTempFile("publicKey", ".pem");
		privateKeyFile.deleteOnExit();
		publicKeyFile.deleteOnExit();

		writePrivateKey(keyPair, privateKeyFile);
		writePublicKey(keyPair, publicKeyFile);

		var provider = KeyProvider.forX25519From(null, privateKeyFile.getAbsolutePath(),
				publicKeyFile.getAbsolutePath());

		assertArrayEquals(keyPair.getPrivate().getEncoded(), provider.getPrivateKey().getEncoded());
		assertArrayEquals(keyPair.getPublic().getEncoded(), provider.getPublicKey().getEncoded());
	}

	private void writePrivateKey(KeyPair keyPair, File file) throws Exception
	{
		writePem("PRIVATE KEY", keyPair.getPrivate().getEncoded(), file);
	}

	private void writePublicKey(KeyPair keyPair, File file) throws Exception
	{
		writePem("PUBLIC KEY", keyPair.getPublic().getEncoded(), file);
	}

	private void writePem(String type, byte[] encoded, File file) throws Exception
	{

		var base64 = Base64.getMimeEncoder(64, System.lineSeparator().getBytes()).encodeToString(encoded);

		var pem = """
				-----BEGIN %s-----
				%s
				-----END %s-----
				""".formatted(type, base64, type);

		Files.writeString(file.toPath(), pem);
	}
}