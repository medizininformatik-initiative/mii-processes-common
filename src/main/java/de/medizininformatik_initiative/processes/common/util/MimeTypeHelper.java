package de.medizininformatik_initiative.processes.common.util;

import java.nio.charset.StandardCharsets;

import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Resource;

import ca.uhn.fhir.context.FhirContext;

public class MimeTypeHelper
{
	public static String getMimeType(Resource resource)
	{
		if (resource instanceof Binary binary)
			return binary.getContentType();
		else
			return "application/fhir+xml";
	}

	public static byte[] getData(FhirContext fhirContext, Resource resource)
	{
		if (resource instanceof Binary binary)
			return binary.getData();
		else
			return fhirContext.newXmlParser().encodeResourceToString(resource).getBytes(StandardCharsets.UTF_8);
	}
}
