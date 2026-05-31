package de.medizininformatik_initiative.processes.common.crypto;

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
	public InputStream encrypt(InputStream plainText, PublicKey publicKey) throws InvalidKeyException,
			NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IOException,
			GeneralSecurityException, KeyNotFoundException, KeyNotSupportedException
	{
		byte[] receiverKeyId = new byte[ProtocolV1.RECEIVER_KEY_ID_LENGTH]; // constant value e.g. all zeros

		Protocol protocol = new ProtocolV1(Mode.base(), KemId.DHKEM_X25519_HKDF_SHA256, KdfId.HKDF_SHA256,
				AeadId.AES_128_GCM, ChunkLength.MiB_1, receiverKeyId);

		Hpke hpke = new Hpke(new ProtocolFactory(PreSharedKeyProvider.of(), ReceiverPrivateKeyProvider.of()));

		return hpke.encrypt(protocol, plainText, publicKey);
	}

	@Override
	public InputStream decrypt(InputStream cryptText, PrivateKey privateKey) throws InvalidKeyException,
			NoSuchAlgorithmException, DecapsulateException, NoSuchPaddingException, InvalidAlgorithmParameterException,
			IOException, GeneralSecurityException, KeyNotFoundException, KeyNotSupportedException
	{
		ReceiverPrivateKeyProvider receiverPrivateKeyProvider = _ -> privateKey;

		Hpke hpke = new Hpke(new ProtocolFactory(PreSharedKeyProvider.of(), receiverPrivateKeyProvider));

		return hpke.decrypt(cryptText);
	}
}
