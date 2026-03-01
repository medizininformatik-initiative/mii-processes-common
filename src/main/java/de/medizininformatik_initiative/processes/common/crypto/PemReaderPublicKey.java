package de.medizininformatik_initiative.processes.common.crypto;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PublicKey;
import java.util.Objects;

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.openssl.PEMException;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

public class PemReaderPublicKey
{
	public static PublicKey readPublicKey(Path pem) throws IOException
	{
		Objects.requireNonNull(pem, "pem");

		try (InputStream in = Files.newInputStream(pem))
		{
			return readPublicKey(in);
		}
	}

	public static PublicKey readPublicKey(InputStream pem) throws IOException
	{
		Objects.requireNonNull(pem, "pem");

		try (Reader reader = new InputStreamReader(pem); PEMParser parser = new PEMParser(reader))
		{
			Object o = parser.readObject();

			if (o instanceof SubjectPublicKeyInfo spki)
			{
				try
				{
					return new JcaPEMKeyConverter().getPublicKey(spki);
				}
				catch (PEMException e)
				{
					throw new IOException(e);
				}
			}
			else
				throw new IOException("Read pem object not a public key");
		}
	}

}
