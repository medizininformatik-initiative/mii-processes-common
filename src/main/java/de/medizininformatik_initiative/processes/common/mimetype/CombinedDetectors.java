package de.medizininformatik_initiative.processes.common.mimetype;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;

public class CombinedDetectors implements Detector
{
	public static CombinedDetectors fromDefaultWithNdJson()
	{
		Detector defaultDetector = TikaConfig.getDefaultConfig().getDetector();
		NdJsonDetector ndJsonDetector = new NdJsonDetector(defaultDetector);

		return new CombinedDetectors(List.of(defaultDetector, ndJsonDetector));
	}

	private final List<Detector> detectors = new ArrayList<>();

	public CombinedDetectors(List<Detector> detectors)
	{
		if (detectors != null && !detectors.isEmpty())
			this.detectors.addAll(detectors);

		if (this.detectors.isEmpty())
			throw new RuntimeException("no detectors supplied");
	}


	/**
	 * @param inputStream
	 *            should be checked for declared and actual mimetype, not <code>null</code>
	 * @param metadata
	 *            providing additional information for check, not <code>null</code>
	 * @return the first mimetype not equal to <code>text/plain</code>, <code>text/plain</code> if no other mimetype is
	 *         detected, <code>application/x-empty</code> if no mimetype could be detected
	 */
	@Override
	public MediaType detect(InputStream inputStream, Metadata metadata)
	{
		List<MediaType> detectedMediaTypesNotEmpty = detectors.stream().map(doDetect(inputStream, metadata))
				.filter(notEqualsMediaType(MediaType.EMPTY)).toList();

		List<MediaType> detectedMediaTypesNotEmptyOrPlainText = detectedMediaTypesNotEmpty.stream()
				.filter(notEqualsMediaType(MediaType.TEXT_PLAIN)).toList();

		if (!detectedMediaTypesNotEmptyOrPlainText.isEmpty())
			return detectedMediaTypesNotEmptyOrPlainText.get(0);

		if (!detectedMediaTypesNotEmpty.isEmpty())
			return detectedMediaTypesNotEmpty.get(0);

		return MediaType.EMPTY;
	}

	private Function<Detector, MediaType> doDetect(InputStream input, Metadata metadata)
	{
		return (detector) ->
		{
			try
			{
				return detector.detect(input, metadata);
			}
			catch (Exception exception)
			{
				return MediaType.EMPTY;
			}
		};
	}

	private Predicate<MediaType> notEqualsMediaType(MediaType toCompare)
	{
		return (mediaType) -> toCompare != null && !toCompare.equals(mediaType);
	}
}
