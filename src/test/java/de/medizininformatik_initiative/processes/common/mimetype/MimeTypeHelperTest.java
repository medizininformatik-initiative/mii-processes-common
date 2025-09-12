package de.medizininformatik_initiative.processes.common.mimetype;

import java.io.ByteArrayInputStream;
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
	public void testAttachmentBundleBytes()
	{
		List<Resource> resources = getResourceNotDocumentReferenceFromPath("/fhir/Bundle/DicFhirStore_Demo_Bundle.xml");
		testResourcesBytes(resources);
	}

	@Test
	public void testAttachmentBundleStream()
	{
		List<Resource> resources = getResourceNotDocumentReferenceFromPath("/fhir/Bundle/DicFhirStore_Demo_Bundle.xml");
		testResourcesStream(resources);
	}

	@Test
	public void testAttachmentCsvBytes()
	{
		List<Resource> resources = getResourceNotDocumentReferenceFromPath("/fhir/Bundle/DicFhirStore_Demo_CSV.xml");
		testResourcesBytes(resources);
	}

	@Test
	public void testAttachmentCsvStream()
	{
		List<Resource> resources = getResourceNotDocumentReferenceFromPath("/fhir/Bundle/DicFhirStore_Demo_CSV.xml");
		testResourcesStream(resources);
	}

	@Test
	public void testAttachmentEvaluationBytes()
	{
		List<Resource> resources = getResourceNotDocumentReferenceFromPath(
				"/fhir/Bundle/DicFhirStore_Demo_Evaluation.xml");
		testResourcesBytes(resources);
	}

	@Test
	public void testAttachmentEvaluationStream()
	{
		List<Resource> resources = getResourceNotDocumentReferenceFromPath(
				"/fhir/Bundle/DicFhirStore_Demo_Evaluation.xml");
		testResourcesStream(resources);
	}

	@Test
	public void testAttachmentTorchBytes()
	{
		List<Resource> resources = getResourceNotDocumentReferenceFromPath("/fhir/Bundle/DicFhirStore_Demo_TORCH.xml");
		testResourcesBytes(resources);
	}

	@Test
	public void testAttachmentTorchStream()
	{
		List<Resource> resources = getResourceNotDocumentReferenceFromPath("/fhir/Bundle/DicFhirStore_Demo_TORCH.xml");
		testResourcesStream(resources);
	}

	@Test
	public void testAttachmentZipBytes()
	{
		List<Resource> resources = getResourceNotDocumentReferenceFromPath("/fhir/Bundle/DicFhirStore_Demo_ZIP.xml");
		testResourcesBytes(resources);
	}

	@Test
	public void testAttachmentZipStream()
	{
		List<Resource> resources = getResourceNotDocumentReferenceFromPath("/fhir/Bundle/DicFhirStore_Demo_ZIP.xml");
		testResourcesStream(resources);
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

	private void testResourcesBytes(List<Resource> resources)
	{
		MimeTypeHelper mimeTypeHelper = createMimetypeHelper();

		for (Resource resource : resources)
		{
			byte[] data = mimeTypeHelper.getData(resource);
			String mimeType = mimeTypeHelper.getMimeType(resource);

			mimeTypeHelper.validate(data, mimeType);
		}
	}

	private void testResourcesStream(List<Resource> resources)
	{
		MimeTypeHelper mimeTypeHelper = createMimetypeHelper();

		for (Resource resource : resources)
		{
			byte[] data = mimeTypeHelper.getData(resource);
			InputStream dataStream = new ByteArrayInputStream(data);
			String mimeType = mimeTypeHelper.getMimeType(resource);

			try
			{
				mimeTypeHelper.validate(dataStream, mimeType);
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		}
	}

	private MimeTypeHelper createMimetypeHelper()
	{
		Detector detector = CombinedDetectors.fromDefaultWithNdJson();
		FhirContext context = FhirContext.forR4();

		return new MimeTypeHelper(detector, context);
	}
}
