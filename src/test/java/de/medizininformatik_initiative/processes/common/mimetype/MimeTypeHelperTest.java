package de.medizininformatik_initiative.processes.common.mimetype;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.tika.detect.Detector;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Resource;
import org.junit.Test;

import ca.uhn.fhir.context.FhirContext;

public class MimeTypeHelperTest
{
	@Test
	public void testAttachmentBundle()
	{
		List<Resource> resources = getResourceNotDocumentReferenceFromPath("/fhir/Bundle/DicFhirStore_Demo_Bundle.xml");
		testResources(resources);
	}

	@Test
	public void testAttachmentCsv()
	{
		List<Resource> resources = getResourceNotDocumentReferenceFromPath("/fhir/Bundle/DicFhirStore_Demo_CSV.xml");
		testResources(resources);
	}

	@Test
	public void testAttachmentEvaluation()
	{
		List<Resource> resources = getResourceNotDocumentReferenceFromPath(
				"/fhir/Bundle/DicFhirStore_Demo_Evaluation.xml");
		testResources(resources);
	}

	@Test
	public void testAttachmentTorch()
	{
		List<Resource> resources = getResourceNotDocumentReferenceFromPath("/fhir/Bundle/DicFhirStore_Demo_TORCH.xml");
		testResources(resources);
	}

	@Test
	public void testAttachmentZip()
	{
		List<Resource> resources = getResourceNotDocumentReferenceFromPath("/fhir/Bundle/DicFhirStore_Demo_ZIP.xml");
		testResources(resources);
	}

	private List<Resource> getResourceNotDocumentReferenceFromPath(String pathToBundle)
	{
		try (InputStream input = getClass().getResourceAsStream(pathToBundle))
		{
			Bundle bundle = FhirContext.forR4().newXmlParser().parseResource(Bundle.class, input);
			return bundle.getEntry().stream().filter(Bundle.BundleEntryComponent::hasResource)
					.map(Bundle.BundleEntryComponent::getResource).filter(r -> !(r instanceof DocumentReference))
					.toList();
		}
		catch (IOException exception)
		{
			throw new RuntimeException(exception);
		}
	}

	private void testResources(List<Resource> resources)
	{
		MimeTypeHelper mimeTypeHelper = createMimetypeHelper();

		for (Resource resource : resources)
		{
			byte[] data = mimeTypeHelper.getData(resource);
			String mimeType = mimeTypeHelper.getMimeType(resource);

			mimeTypeHelper.validate(data, mimeType);
		}
	}

	private MimeTypeHelper createMimetypeHelper()
	{
		Detector detector = CombinedDetectors.fromDefaultWithNdJson();
		FhirContext context = FhirContext.forR4();

		return new MimeTypeHelper(detector, context);
	}
}
