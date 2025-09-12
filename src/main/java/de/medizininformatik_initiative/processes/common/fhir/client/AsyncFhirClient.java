package de.medizininformatik_initiative.processes.common.fhir.client;

import org.hl7.fhir.r4.model.Resource;

public interface AsyncFhirClient extends FhirClient
{
	/**
	 * Search async using by entering a search URL directly. This follows the
	 * <a href="http://hl7.org/fhir/R5/async.html">asynchronous request pattern</a> specified in FHIR R5.
	 *
	 * @param url
	 *            The URL to search for. This URL may be complete (e.g. "http://example.com/base/Patient?name=foo") in
	 *            which case the base URL will be ignored.
	 * @return not <code>null</code>
	 */
	Resource search(String url);
}
