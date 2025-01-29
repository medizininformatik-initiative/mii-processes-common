package de.medizininformatik_initiative.processes.common.crypto;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;

import org.junit.Test;

public class RsaAesGcmUtilTest
{
	@Test
	public void testEncryptionDecryptionBytesToBytes() throws Exception
	{
		String data = "bytes-to-bytes";
		byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);

		String sendingOrganizationIdentifier = "sending";
		String receivingOrganizationIdentifier = "receiving";

		KeyPair keyPair = RsaAesGcmUtil.generateRsa4096KeyPair();

		byte[] encrypted = RsaAesGcmUtil.encrypt(keyPair.getPublic(), dataBytes, sendingOrganizationIdentifier,
				receivingOrganizationIdentifier);
		byte[] decrypted = RsaAesGcmUtil.decrypt(keyPair.getPrivate(), encrypted, sendingOrganizationIdentifier,
				receivingOrganizationIdentifier);

		String dataDecrypted = new String(decrypted, StandardCharsets.UTF_8);

		assertEquals(data, dataDecrypted);
	}

	@Test
	public void testEncryptionDecryptionBytesToStream() throws Exception
	{
		String data = "bytes-to-stream";
		byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);

		String sendingOrganizationIdentifier = "sending";
		String receivingOrganizationIdentifier = "receiving";

		KeyPair keyPair = RsaAesGcmUtil.generateRsa4096KeyPair();

		byte[] encrypted = RsaAesGcmUtil.encrypt(keyPair.getPublic(), dataBytes, sendingOrganizationIdentifier,
				receivingOrganizationIdentifier);
		InputStream encryptedStream = new ByteArrayInputStream(encrypted);
		InputStream decrypted = RsaAesGcmUtil.decrypt(keyPair.getPrivate(), encryptedStream,
				sendingOrganizationIdentifier, receivingOrganizationIdentifier);

		byte[] dataBytesDecrypted = decrypted.readAllBytes();
		String dataDecrypted = new String(dataBytesDecrypted, StandardCharsets.UTF_8);

		assertEquals(data, dataDecrypted);
	}

	@Test
	public void testEncryptionDecryptionStreamToBytes() throws Exception
	{
		String data = "stream-to-bytes";
		byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
		InputStream dataStream = new ByteArrayInputStream(dataBytes);

		String sendingOrganizationIdentifier = "sending";
		String receivingOrganizationIdentifier = "receiving";

		KeyPair keyPair = RsaAesGcmUtil.generateRsa4096KeyPair();

		InputStream encrypted = RsaAesGcmUtil.encrypt(keyPair.getPublic(), dataStream, sendingOrganizationIdentifier,
				receivingOrganizationIdentifier);
		byte[] encryptedBytes = encrypted.readAllBytes();
		byte[] decrypted = RsaAesGcmUtil.decrypt(keyPair.getPrivate(), encryptedBytes, sendingOrganizationIdentifier,
				receivingOrganizationIdentifier);

		String dataDecrypted = new String(decrypted, StandardCharsets.UTF_8);

		assertEquals(data, dataDecrypted);
	}

	@Test
	public void testEncryptionDecryptionStreamToStream() throws Exception
	{
		String data = "stream-to-stream";
		byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
		InputStream dataStream = new ByteArrayInputStream(dataBytes);

		String sendingOrganizationIdentifier = "sending";
		String receivingOrganizationIdentifier = "receiving";

		KeyPair keyPair = RsaAesGcmUtil.generateRsa4096KeyPair();

		InputStream encrypted = RsaAesGcmUtil.encrypt(keyPair.getPublic(), dataStream, sendingOrganizationIdentifier,
				receivingOrganizationIdentifier);
		InputStream decrypted = RsaAesGcmUtil.decrypt(keyPair.getPrivate(), encrypted, sendingOrganizationIdentifier,
				receivingOrganizationIdentifier);

		byte[] dataBytesDecrypted = decrypted.readAllBytes();
		String dataDecrypted = new String(dataBytesDecrypted, StandardCharsets.UTF_8);

		assertEquals(data, dataDecrypted);
	}
}
