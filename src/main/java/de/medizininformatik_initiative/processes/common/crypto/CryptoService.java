package de.medizininformatik_initiative.processes.common.crypto;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

import javax.crypto.DecapsulateException;
import javax.crypto.NoSuchPaddingException;

import de.hsheilbronn.mi.utils.crypto.hpke.KeyNotFoundException;
import de.hsheilbronn.mi.utils.crypto.hpke.KeyNotSupportedException;
import de.hsheilbronn.mi.utils.crypto.hpke.ReceiverPrivateKeyProvider;

public interface CryptoService
{
	default byte[] encrypt(byte[] plainText, PublicKey publicKey, String receiverKeyId) throws InvalidKeyException,
			NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IOException,
			GeneralSecurityException, KeyNotFoundException, KeyNotSupportedException
	{
		return encrypt(new ByteArrayInputStream(plainText), publicKey, receiverKeyId).readAllBytes();
	}

	InputStream encrypt(InputStream plainText, PublicKey publicKey, String receiverKeyId) throws InvalidKeyException,
			NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IOException,
			GeneralSecurityException, KeyNotFoundException, KeyNotSupportedException;

	default byte[] decrypt(byte[] cryptText, ReceiverPrivateKeyProvider receiverPrivateKeyProvider)
			throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidAlgorithmParameterException, IOException, GeneralSecurityException, KeyNotFoundException,
			KeyNotSupportedException
	{
		return decrypt(new ByteArrayInputStream(cryptText), receiverPrivateKeyProvider).readAllBytes();
	}

	InputStream decrypt(InputStream cryptText, ReceiverPrivateKeyProvider receiverPrivateKeyProvider)
			throws InvalidKeyException, NoSuchAlgorithmException, DecapsulateException, NoSuchPaddingException,
			InvalidAlgorithmParameterException, IOException, GeneralSecurityException, KeyNotFoundException,
			KeyNotSupportedException;


	static CryptoService x25519()
	{
		return new CryptoServiceX25519();
	}
}
