package de.medizininformatik_initiative.processes.common.crypto;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AesGcmUtil
{
	private static final String AES = "AES";
	private static final String AES_MODE_PADDING = "AES/GCM/NoPadding";
	private static final int AES_KEY_SIZE = 256;
	private static final int GCM_IV_LENGTH = 12;
	private static final int GCM_TAG_LENGTH = 128;

	private static final SecureRandom random = new SecureRandom();

	public static SecretKey generateAES256Key() throws NoSuchAlgorithmException
	{
		KeyGenerator keyGen = KeyGenerator.getInstance(AES);
		keyGen.init(AES_KEY_SIZE);
		return keyGen.generateKey();
	}

	public static byte[] generateIv(int ivLength)
	{
		byte[] bytes = new byte[ivLength];
		random.nextBytes(bytes);
		return bytes;
	}

	public static byte[] encrypt(byte[] message, byte[] aadTag, SecretKey key)
			throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
			InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException
	{
		byte[] iv = generateIv(GCM_IV_LENGTH);

		SecretKeySpec keySpec = new SecretKeySpec(key.getEncoded(), AES);
		GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

		Cipher cipher = Cipher.getInstance(AES_MODE_PADDING);
		cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmParameterSpec);
		cipher.updateAAD(aadTag);

		byte[] encrypted = cipher.doFinal(message);

		byte[] output = new byte[iv.length + encrypted.length];
		System.arraycopy(iv, 0, output, 0, iv.length);
		System.arraycopy(encrypted, 0, output, iv.length, encrypted.length);

		return output;
	}

	public static InputStream encrypt(InputStream message, byte[] aadTag, SecretKey key)
			throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
			InvalidAlgorithmParameterException
	{
		byte[] iv = generateIv(GCM_IV_LENGTH);
		InputStream ivStream = new ByteArrayInputStream(iv);

		SecretKeySpec keySpec = new SecretKeySpec(key.getEncoded(), AES);
		GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

		Cipher cipher = Cipher.getInstance(AES_MODE_PADDING);
		cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmParameterSpec);
		cipher.updateAAD(aadTag);

		InputStream encryptedStream = new CipherInputStream(message, cipher);

		return new SequenceInputStream(ivStream, encryptedStream);
	}

	public static byte[] decrypt(byte[] message, byte[] aadTag, SecretKey key)
			throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
			InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException
	{
		SecretKeySpec keySpec = new SecretKeySpec(key.getEncoded(), AES);
		GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, message, 0, GCM_IV_LENGTH);

		Cipher cipher = Cipher.getInstance(AES_MODE_PADDING);
		cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmParameterSpec);
		cipher.updateAAD(aadTag);

		return cipher.doFinal(message, GCM_IV_LENGTH, message.length - GCM_IV_LENGTH);
	}

	public static InputStream decrypt(InputStream message, byte[] aadTag, SecretKey key)
			throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
			InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, IOException
	{
		SecretKeySpec keySpec = new SecretKeySpec(key.getEncoded(), AES);

		byte[] iv = new byte[GCM_IV_LENGTH];
		int bytesRead = message.read(iv);

		if (bytesRead != GCM_IV_LENGTH)
			throw new IOException("Failed to read the complete encrypted AES key");

		GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

		Cipher cipher = Cipher.getInstance(AES_MODE_PADDING);
		cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmParameterSpec);
		cipher.updateAAD(aadTag);

		return new CipherInputStream(message, cipher);
	}
}