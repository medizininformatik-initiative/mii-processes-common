package de.medizininformatik_initiative.processes.common.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Test;

import de.rwh.utils.crypto.io.PemIo;

public class KeyProviderImplTest
{

	@Test
	public void errorOnPrivateAndPublicKeyFilesAreNull() throws Exception
	{
		assertThatThrownBy(() -> KeyProviderImpl.fromFiles(null, null, null, null))
				.hasMessage("privateKeyFile path must not be null");
	}

	@Test
	public void errorOnPrivateKeyFileIsNull() throws Exception
	{
		var generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(1024);
		var nonMatchingPublicKey = (RSAPublicKey) generator.generateKeyPair().getPublic();
		var publicKeyFile = File.createTempFile("publicKey", ".pem");
		publicKeyFile.deleteOnExit();
		PemIo.writePublicKeyToPem(nonMatchingPublicKey, publicKeyFile.toPath());

		assertThatThrownBy(() -> KeyProviderImpl.fromFiles(null, null, publicKeyFile.getAbsolutePath(), null))
				.hasMessage("privateKeyFile path must not be null");
	}

	@Test
	public void errorOnPublicKeyFileIsNull() throws Exception
	{
		var generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(1024);
		var privateKey = (RSAPrivateKey) generator.generateKeyPair().getPrivate();
		var privateKeyFile = File.createTempFile("privateKey", ".pem");
		privateKeyFile.deleteOnExit();
		PemIo.writeNotEncryptedPrivateKeyToOpenSslClassicPem(new BouncyCastleProvider(), privateKeyFile.toPath(),
				privateKey);

		assertThatThrownBy(() -> KeyProviderImpl.fromFiles(null, privateKeyFile.getAbsolutePath(), null, null))
				.hasMessage("publicKeyFile path must not be null");
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
		PemIo.writePublicKeyToPem(nonMatchingPublicKey, publicKeyFile.toPath());
		PemIo.writeNotEncryptedPrivateKeyToOpenSslClassicPem(new BouncyCastleProvider(), privateKeyFile.toPath(),
				privateKey);

		assertThat(privateKey.getModulus()).describedAs("private and public keys do not match")
				.isNotEqualByComparingTo(nonMatchingPublicKey.getModulus());
		assertThatThrownBy(() -> KeyProviderImpl.fromFiles(null, privateKeyFile.getAbsolutePath(),
				publicKeyFile.getAbsolutePath(), null))
				.hasMessage("PrivateKey '%s' and PublicKey '%s' do not match."
						.formatted(privateKeyFile.getAbsolutePath(), publicKeyFile.getAbsolutePath()));
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
		PemIo.writePublicKeyToPem(nonMatchingPublicKey, publicKeyFile.toPath());
		PemIo.writeNotEncryptedPrivateKeyToOpenSslClassicPem(new BouncyCastleProvider(), privateKeyFile.toPath(),
				privateKey);

		assertThat(privateKey).isNotInstanceOf(RSAPrivateKey.class);
		assertThatThrownBy(() -> KeyProviderImpl.fromFiles(null, privateKeyFile.getAbsolutePath(),
				publicKeyFile.getAbsolutePath(), null))
				.hasMessageContaining("is not an RSA based private key. Only RSA is supported.");
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
		PemIo.writePublicKeyToPem((RSAPublicKey) matchingPublicKey, publicKeyFile.toPath());
		PemIo.writeNotEncryptedPrivateKeyToOpenSslClassicPem(new BouncyCastleProvider(), privateKeyFile.toPath(),
				privateKey);

		var provider = KeyProviderImpl.fromFiles(null, privateKeyFile.getAbsolutePath(),
				publicKeyFile.getAbsolutePath(), null);

		assertThat(((RSAPrivateKey) provider.getPrivateKey()).getModulus()).describedAs("same private key")
				.isEqualByComparingTo(privateKey.getModulus());
		assertThat(((RSAPublicKey) provider.getPublicKey()).getModulus()).describedAs("same public key")
				.isEqualByComparingTo(matchingPublicKey.getModulus());
		assertThat(((RSAPrivateKey) provider.getPrivateKey()).getModulus()).describedAs("private and public keys match")
				.isEqualByComparingTo(((RSAPublicKey) provider.getPublicKey()).getModulus());
	}
}
