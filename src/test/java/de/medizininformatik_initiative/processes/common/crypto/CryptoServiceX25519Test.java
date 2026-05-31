package de.medizininformatik_initiative.processes.common.crypto;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;

import org.junit.Test;

import de.hsheilbronn.mi.utils.crypto.hpke.KeyNotFoundException;
import de.hsheilbronn.mi.utils.crypto.hpke.KeyNotSupportedException;
import de.hsheilbronn.mi.utils.crypto.keypair.KeyPairGeneratorFactory;

public class CryptoServiceX25519Test
{
	@Test
	public void encryptDecrypt()
			throws KeyNotSupportedException, GeneralSecurityException, IOException, KeyNotFoundException
	{
		KeyPair keyPair = KeyPairGeneratorFactory.x25519().initialize().generateKeyPair();
		String text = "Foo, Bar, Baz";

		CryptoService service = CryptoService.x25519();

		InputStream encrypted = service.encrypt(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)),
				keyPair.getPublic());
		InputStream decrypted = service.decrypt(encrypted, keyPair.getPrivate());
		assertEquals(text, new String(decrypted.readAllBytes(), StandardCharsets.UTF_8));
	}
}
