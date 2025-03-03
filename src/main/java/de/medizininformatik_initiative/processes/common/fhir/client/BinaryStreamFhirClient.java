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
	default InputStream read(IdType idType, String mimeType)
	{
		return read(idType, mimeType, false);
	}

	/**
	 * Method as workaround for HAPI Binary streaming deviation from FHIR standard: <a href=
	 * "https://github.com/hapifhir/hapi-fhir-jpaserver-starter/issues/179">https://github.com/hapifhir/hapi-fhir-jpaserver-starter/issues/179</a>
	 * <p>
	 * Works as {@link #read(IdType, String)}, except it additionally uses the <code>$binary-access-read</code>
	 * operation if parameter <code>useHapiDatabaseBlobStorageOperation</code> is set to <code>true</code>, e.g. GET
	 * http://foo.bar/fhir/Binary/1/$binary-access-read.
	 *
	 * @param useHapiDatabaseBlobStorageOperation
	 *            set to <code>true</code> if HAPI uses an external Binary storage solution by setting the ENV variable
	 *            <code>HAPI_FHIR_BINARY_STORAGE_ENABLED</code>
	 */
	InputStream read(IdType idType, String mimeType, boolean useHapiDatabaseBlobStorageOperation);

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
