package de.medizininformatik_initiative.processes.common.fhir.client;

import java.io.InputStream;

import org.hl7.fhir.r4.model.IdType;

import ca.uhn.fhir.rest.api.MethodOutcome;

public interface BinaryStreamFhirClient extends FhirClient
{
	/**
	 * Reading a Binary resource content based on {@link org.hl7.fhir.r4.model.IdType#getResourceType()} and
	 * {@link org.hl7.fhir.r4.model.IdType#getIdPart()} and an optional
	 * {@link org.hl7.fhir.r4.model.IdType#getVersionIdPart()}.
	 *
	 * @param idType
	 *            not <code>null</code>, {@link org.hl7.fhir.r4.model.IdType#getResourceType()} not <code>null</code> or
	 *            empty, and must be of type 'Binary', {@link org.hl7.fhir.r4.model.IdType#getIdPart()}, not
	 *            <code>null</code> or empty, {@link org.hl7.fhir.r4.model.IdType#getVersionIdPart()} may be
	 *            <code>null</code> or empty
	 * @param mimeType
	 *            not <code>null</code>
	 * @return not <code>null</code>
	 */
	InputStream read(IdType idType, String mimeType);

	/**
	 * Creating Binary resource content.
	 *
	 * @param stream
	 *            not <code>null</code>
	 * @param mimeType
	 *            not <code>null</code>
	 * @return not <code>null</code>
	 */
	MethodOutcome create(InputStream stream, String mimeType);
}
