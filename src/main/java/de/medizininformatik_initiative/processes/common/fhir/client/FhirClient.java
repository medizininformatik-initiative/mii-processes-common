package de.medizininformatik_initiative.processes.common.fhir.client;

import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Resource;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;

public interface FhirClient
{
	/**
	 * @return not <code>null</code> or empty
	 */
	String getLocalIdentifierValue();

	/**
	 * @return not <code>null</code>
	 */
	FhirContext getFhirContext();

	/**
	 * Base URL of the FHIR server this client connects to.
	 *
	 * @return not <code>null</code> or empty
	 */
	String getFhirBaseUrl();

	/**
	 * The {@link ca.uhn.fhir.rest.client.api.IGenericClient to access the FHIR server with base Url defined by
	 * {@link #getFhirBaseUrl()}}.
	 *
	 * @return not <code>null</code>
	 */
	IGenericClient getGenericFhirClient();

	/**
	 * Testing connection to the FHIR server CapabilityStatement using the base URL defined by
	 * {@link #getFhirBaseUrl()}.
	 */
	void testConnection();

	/**
	 * Reading a resource based on {@link org.hl7.fhir.r4.model.IdType#getResourceType()} and
	 * {@link org.hl7.fhir.r4.model.IdType#getIdPart()} and an optional
	 * {@link org.hl7.fhir.r4.model.IdType#getVersionIdPart()}.
	 *
	 * @param idType
	 *            not <code>null</code> {@link org.hl7.fhir.r4.model.IdType#getResourceType()}, not <code>null</code> or
	 *            empty {@link org.hl7.fhir.r4.model.IdType#getIdPart()}, may be <code>null</code> or empty
	 *            {@link org.hl7.fhir.r4.model.IdType#getVersionIdPart()},
	 * @return not <code>null</code>
	 */
	Resource read(IdType idType);

	/**
	 * Reading a Binary resource based on {@link org.hl7.fhir.r4.model.IdType#getIdPart()} and an optional
	 * {@link org.hl7.fhir.r4.model.IdType#getVersionIdPart()}.
	 *
	 * @param idType
	 *            not <code>null</code> or empty {@link org.hl7.fhir.r4.model.IdType#getIdPart()}, may be
	 *            <code>null</code> or empty {@link org.hl7.fhir.r4.model.IdType#getVersionIdPart()},
	 * @return not <code>null</code>
	 */
	Binary readBinary(IdType idType);

	/**
	 * Simply search by entering a search URL directly.
	 *
	 * @param url
	 *            The URL to search for. This URL may be complete (e.g. "http://example.com/base/Patient?name=foo") in
	 *            which case the base URL will be ignored. Or it can be relative (e.g. "Patient?name=foo") in which case
	 *            the client's base URL will be used.
	 * @return The search result as {@link Bundle}
	 */
	Resource search(String url);

	/**
	 * Searching for a DocumentResource having an {@link org.hl7.fhir.r4.model.Identifier} matching the provided system
	 * and code.
	 *
	 * @param system
	 *            not <code>null</code> or empty
	 * @param code
	 *            not <code>null</code> or empty
	 * @return {@link org.hl7.fhir.r4.model.Bundle} of type {@link org.hl7.fhir.r4.model.Bundle.BundleType#SEARCHSET}
	 *         containing the matching {@link org.hl7.fhir.r4.model.DocumentReference}, no not <code>null</code>
	 */
	Bundle searchDocumentReferences(String system, String code);

	/**
	 * Executing a {@link org.hl7.fhir.r4.model.Bundle} of type
	 * {@link org.hl7.fhir.r4.model.Bundle.BundleType#TRANSACTION} containing one or multiple requests.
	 *
	 * @param bundle
	 *            of type {@link org.hl7.fhir.r4.model.Bundle.BundleType#TRANSACTION}, not <code>null</code>
	 * @return {@link org.hl7.fhir.r4.model.Bundle} of type
	 *         {@link org.hl7.fhir.r4.model.Bundle.BundleType#TRANSACTIONRESPONSE} containing * the responses of each
	 *         executed request defined in the {@param bundle}, no not <code>null</code>
	 */
	Bundle executeTransaction(Bundle bundle);

	/**
	 * Executing a {@link org.hl7.fhir.r4.model.Bundle} of type {@link org.hl7.fhir.r4.model.Bundle.BundleType#BATCH}
	 * containing one or multiple requests.
	 *
	 * @param bundle
	 *            of type {@link org.hl7.fhir.r4.model.Bundle.BundleType#BATCH}, not <code>null</code>
	 * @return {@link org.hl7.fhir.r4.model.Bundle} of type
	 *         {@link org.hl7.fhir.r4.model.Bundle.BundleType#BATCHRESPONSE} containing * the responses of each executed
	 *         request defined in the {@param bundle}, no not <code>null</code>
	 */
	Bundle executeBatch(Bundle bundle);

	/**
	 * @param resource
	 *            not <code>null</code>
	 * @return not <code>null</code>
	 */
	MethodOutcome create(Resource resource);
}
