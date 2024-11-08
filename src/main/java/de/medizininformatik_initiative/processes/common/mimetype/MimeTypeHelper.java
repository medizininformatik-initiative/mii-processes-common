package de.medizininformatik_initiative.processes.common.mimetype;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ca.uhn.fhir.context.FhirContext;

public class MimeTypeHelper implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(MimeTypeHelper.class);

	private final Detector detector;
	private final FhirContext fhirContext;

	/**
	 * @deprecated use {@link MimeTypeHelper(Detector, FhirContext)} instead. Uses
	 *             {@link CombinedDetectors#fromDefaultWithNdJson()}.
	 */
	@Deprecated
	public MimeTypeHelper(FhirContext fhirContext)
	{
		this(CombinedDetectors.fromDefaultWithNdJson(), fhirContext);
	}

	public MimeTypeHelper(Detector detector, FhirContext fhirContext)
	{
		this.detector = detector;
		this.fhirContext = fhirContext;
	}

	@Override
	public void afterPropertiesSet()
	{
		Objects.requireNonNull(detector, "detector");
		Objects.requireNonNull(fhirContext, "fhirContext");
	}

	/**
	 * Detects the mime-type of the provided data and validates if the detected mime-type equals the declared mime-type.
	 * Logs a warning if the full mime-types do not match, throws a {@link RuntimeException} if the base mime-types do
	 * not match.
	 *
	 * @param data
	 *            of which the mime-type should be detected
	 * @param declared
	 *            the declared mime-type of the data
	 * @throws RuntimeException
	 *             if the detected and the declared base mime-type do not match
	 */
	public void validate(byte[] data, String declared)
	{
		MediaType declaredMimeType = MediaType.parse(declared);
		MediaType detectedMimeType = MediaType.EMPTY;

		try
		{
			TikaInputStream input = TikaInputStream.get(data);

			// Gives only a hint to the possible mime-type, this is needed because text/csv and application/json
			// cannot be detected without any hint and would resolve to text/plain.
			Metadata metadata = new Metadata();
			metadata.add(Metadata.CONTENT_TYPE, declaredMimeType.toString());

			detectedMimeType = detector.detect(input, metadata);
		}
		catch (Exception exception)
		{
			throw new RuntimeException("Could not detect mime-type of resource", exception);
		}

		if (!declaredMimeType.equals(detectedMimeType))
			logger.warn("Declared mime-type {} does not match detected mime-type {}", declaredMimeType.toString(),
					detectedMimeType.toString());

		if (!declaredMimeType.getType().equals(detectedMimeType.getType()))
		{
			throw new RuntimeException("Declared base mime-type of '" + declaredMimeType.toString()
					+ "' does not match detected base mime-type of '" + detectedMimeType.toString() + "'");
		}
	}

	/**
	 * Determines the mime-type given the provided resource
	 *
	 * @param resource
	 *            from which the mime-type should be determined, not <code>null</code>
	 * @return if the resource is of type {@link Binary} it returns {@link Binary#getContentType()} else
	 *         <code>application/fhir+xml</code>
	 */
	public String getMimeType(Resource resource)
	{
		if (resource instanceof Binary binary)
			return binary.getContentType();
		else
			return "application/fhir+xml";
	}

	/**
	 * Transforms the resource to a byte[]
	 *
	 * @param resource
	 *            which should be transformed to byte[], not <code>null</code>
	 * @return if the resource is of type {@link Binary} it returns {@link Binary#getData()} else the String-XML
	 *         representation of the resource as byte[]
	 */
	public byte[] getData(Resource resource)
	{
		if (resource instanceof Binary binary)
			return binary.getData();
		else
			return fhirContext.newXmlParser().encodeResourceToString(resource).getBytes(StandardCharsets.UTF_8);
	}
}
