package de.medizininformatik_initiative.processes.common.crypto;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.DecapsulateException;
import javax.crypto.NoSuchPaddingException;

import de.hsheilbronn.mi.utils.crypto.hpke.KeyNotFoundException;
import de.hsheilbronn.mi.utils.crypto.hpke.KeyNotSupportedException;

public interface CryptoService
{
	default byte[] encrypt(byte[] plainText, PublicKey publicKey) throws InvalidKeyException, NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidAlgorithmParameterException, IOException, GeneralSecurityException,
			KeyNotFoundException, KeyNotSupportedException
	{
		return encrypt(new ByteArrayInputStream(plainText), publicKey).readAllBytes();
	}

	InputStream encrypt(InputStream plainText, PublicKey publicKey) throws InvalidKeyException,
			NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IOException,
			GeneralSecurityException, KeyNotFoundException, KeyNotSupportedException;

	default byte[] decrypt(byte[] cryptText, PrivateKey privateKey) throws InvalidKeyException,
			NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IOException,
			GeneralSecurityException, KeyNotFoundException, KeyNotSupportedException
	{
		return decrypt(new ByteArrayInputStream(cryptText), privateKey).readAllBytes();
	}

	InputStream decrypt(InputStream cryptText, PrivateKey privateKey) throws InvalidKeyException,
			NoSuchAlgorithmException, DecapsulateException, NoSuchPaddingException, InvalidAlgorithmParameterException,
			IOException, GeneralSecurityException, KeyNotFoundException, KeyNotSupportedException;


	static CryptoService x25519()
	{
		return new CryptoServiceX25519();
	}
}
