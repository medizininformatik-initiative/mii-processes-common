package de.medizininformatik_initiative.processes.common.fhir.client;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Resource;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;

public interface StandardFhirClient extends FhirClient
{
	/**
	 * The {@link ca.uhn.fhir.rest.client.api.IGenericClient to access the FHIR server with base Url defined by
	 * {@link #getFhirBaseUrl()}}.
	 *
	 * @return not <code>null</code>
	 */
	IGenericClient getGenericFhirClient();

	/**
	 * Reading a resource based on {@link org.hl7.fhir.r4.model.IdType#getResourceType()} and
	 * {@link org.hl7.fhir.r4.model.IdType#getIdPart()} and an optional
	 * {@link org.hl7.fhir.r4.model.IdType#getVersionIdPart()}.
	 *
	 * @param idType
	 *            not <code>null</code>, {@link org.hl7.fhir.r4.model.IdType#getResourceType()} not <code>null</code> or
	 *            empty, {@link org.hl7.fhir.r4.model.IdType#getIdPart()}, not <code>null</code> or empty,
	 *            {@link org.hl7.fhir.r4.model.IdType#getVersionIdPart()} may be <code>null</code> or empty
	 * @return not <code>null</code>
	 */
	Resource read(IdType idType);

	/**
	 * Creating a resource.
	 *
	 * @param resource
	 *            not <code>null</code>
	 * @return not <code>null</code>
	 */
	MethodOutcome create(Resource resource);

	/**
	 * Simply search by entering a search URL directly.
	 *
	 * @param url
	 *            The URL to search for. This URL may be complete (e.g. "http://example.com/base/Patient?name=foo") in
	 *            which case the base URL will be ignored.
	 * @return not <code>null</code>
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
	 *         containing the matching {@link org.hl7.fhir.r4.model.DocumentReference}, not <code>null</code>
	 */
	Bundle searchDocumentReferences(String system, String code);
}
