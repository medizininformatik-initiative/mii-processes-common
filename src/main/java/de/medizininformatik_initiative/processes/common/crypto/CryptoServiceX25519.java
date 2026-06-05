package de.medizininformatik_initiative.processes.common.crypto;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

import javax.crypto.DecapsulateException;
import javax.crypto.NoSuchPaddingException;

import de.hsheilbronn.mi.utils.crypto.hpke.AeadId;
import de.hsheilbronn.mi.utils.crypto.hpke.ChunkLength;
import de.hsheilbronn.mi.utils.crypto.hpke.Hpke;
import de.hsheilbronn.mi.utils.crypto.hpke.KdfId;
import de.hsheilbronn.mi.utils.crypto.hpke.KemId;
import de.hsheilbronn.mi.utils.crypto.hpke.KeyNotFoundException;
import de.hsheilbronn.mi.utils.crypto.hpke.KeyNotSupportedException;
import de.hsheilbronn.mi.utils.crypto.hpke.Mode;
import de.hsheilbronn.mi.utils.crypto.hpke.PreSharedKeyProvider;
import de.hsheilbronn.mi.utils.crypto.hpke.Protocol;
import de.hsheilbronn.mi.utils.crypto.hpke.ProtocolFactory;
import de.hsheilbronn.mi.utils.crypto.hpke.ProtocolV1;
import de.hsheilbronn.mi.utils.crypto.hpke.ReceiverPrivateKeyProvider;

public class CryptoServiceX25519 implements CryptoService
{
	public CryptoServiceX25519()
	{
	}

	@Override
	public InputStream encrypt(InputStream plainText, PublicKey publicKey, String receiverKeyId)
			throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidAlgorithmParameterException, IOException, GeneralSecurityException, KeyNotFoundException,
			KeyNotSupportedException
	{
		byte[] receiverKeyIdBytes = sha256(receiverKeyId);

		Protocol protocol = new ProtocolV1(Mode.base(), KemId.DHKEM_X25519_HKDF_SHA256, KdfId.HKDF_SHA256,
				AeadId.AES_128_GCM, ChunkLength.MiB_1, receiverKeyIdBytes);

		Hpke hpke = new Hpke(new ProtocolFactory(PreSharedKeyProvider.of(), ReceiverPrivateKeyProvider.of()));
		return hpke.encrypt(protocol, plainText, publicKey);
	}

	private byte[] sha256(String input)
	{
		try
		{
			MessageDigest digest = MessageDigest.getInstance("SHA-256"); // = 32 bytes
			return digest.digest(input.getBytes(StandardCharsets.UTF_8));
		}
		catch (NoSuchAlgorithmException e)
		{
			throw new RuntimeException("SHA-256 not available", e);
		}
	}

	@Override
	public InputStream decrypt(InputStream cryptText, ReceiverPrivateKeyProvider receiverPrivateKeyProvider)
			throws InvalidKeyException, NoSuchAlgorithmException, DecapsulateException, NoSuchPaddingException,
			InvalidAlgorithmParameterException, IOException, GeneralSecurityException, KeyNotFoundException,
			KeyNotSupportedException
	{
		Hpke hpke = new Hpke(new ProtocolFactory(PreSharedKeyProvider.of(), receiverPrivateKeyProvider));
		return hpke.decrypt(cryptText);
	}
}
