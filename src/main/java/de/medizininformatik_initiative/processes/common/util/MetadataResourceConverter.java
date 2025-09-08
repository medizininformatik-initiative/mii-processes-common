package de.medizininformatik_initiative.processes.common.util;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.beans.factory.InitializingBean;

import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.ProcessPluginDefinition;

public class MetadataResourceConverter implements InitializingBean
{
	private record MinorMajorVersion(int major, int minor)
	{
		private static MinorMajorVersion from(String version)
		{
			if (version.matches(ProcessPluginDefinition.RESOURCE_VERSION_PATTERN_STRING))
			{
				String[] minorMajor = version.split("\\.");
				return new MinorMajorVersion(Integer.parseInt(minorMajor[0]), Integer.parseInt(minorMajor[1]));
			}

			throw new RuntimeException("Fhir resource version " + version + " does not match regex \\d\\.\\d");
		}

		private static <T extends MetadataResource> Comparator<T> byVersionAsc()
		{
			return (a, b) ->
			{
				MinorMajorVersion va = from(a.getVersion());
				MinorMajorVersion vb = from(b.getVersion());

				int byMajor = Integer.compare(va.major, vb.major);
				if (byMajor != 0)
					return byMajor;
				return Integer.compare(va.minor, vb.minor);
			};
		}

		@SuppressWarnings("unchecked")
		private static <T extends MetadataResource> Comparator<T> byVersionDesc()
		{
			return (Comparator<T>) byVersionAsc().reversed();
		}
	}

	private final ProcessPluginApi api;
	private final String resourcesVersion;

	public MetadataResourceConverter(ProcessPluginApi api, String resourcesVersion)
	{
		this.api = api;
		this.resourcesVersion = resourcesVersion;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(api, "api");
		Objects.requireNonNull(resourcesVersion, "resourcesVersion");
	}

	public <T extends MetadataResource> void searchAndConvertOlderResourcesIfCurrentIsNewestResource(String url,
			Class<T> type, BiConsumer<T, List<T>> converter)
	{
		Bundle searchResult = search(type, url);
		List<T> resourcesSortedDesc = extractResourcesAndSortDesc(searchResult, type, url);

		if (currentIsNewestResourceAndOlderResourcesExist(resourcesSortedDesc))
		{
			T currentResource = resourcesSortedDesc.get(0);
			List<T> olderResources = resourcesSortedDesc.stream().skip(1).toList();

			converter.accept(currentResource, olderResources);
		}
	}

	private Bundle search(Class<? extends Resource> type, String url)
	{
		return api.getFhirWebserviceClientProvider().getLocalWebserviceClient().search(type,
				Map.of("url", List.of(url)));
	}

	private <T extends MetadataResource> List<T> extractResourcesAndSortDesc(Bundle bundle, Class<T> type, String url)
	{
		return bundle.getEntry().stream().filter(Bundle.BundleEntryComponent::hasResource)
				.map(Bundle.BundleEntryComponent::getResource).filter(type::isInstance).map(type::cast)
				.filter(m -> url.equals(m.getUrl())).sorted(MinorMajorVersion.byVersionDesc()).toList();
	}

	private boolean currentIsNewestResourceAndOlderResourcesExist(
			List<? extends MetadataResource> allResourcesSortedDesc)
	{
		if (allResourcesSortedDesc.size() <= 1)
			return false;

		return resourcesVersion.equals(allResourcesSortedDesc.get(0).getVersion());
	}
}
