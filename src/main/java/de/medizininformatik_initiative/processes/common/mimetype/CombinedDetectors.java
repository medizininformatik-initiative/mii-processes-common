package de.medizininformatik_initiative.processes.common.mimetype;

import java.io.IOException;
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

	@Override
	public MediaType detect(InputStream inputStream, Metadata metadata) throws IOException
	{
		// Each detector is responsible to mark and reset the inputstream them self
		// and to check if the inputstream is null

		List<MediaType> detectedMediaTypesNotEmptyNotOctetStream = detectors.stream()
				.map(doDetect(inputStream, metadata)).filter(notEqualsMediaType(MediaType.EMPTY))
				.filter(notEqualsMediaType(MediaType.OCTET_STREAM)).toList();

		List<MediaType> detectedMediaTypesNotEmptyNotOctetStreamNotPlainText = detectedMediaTypesNotEmptyNotOctetStream.stream()
				.filter(notEqualsMediaType(MediaType.TEXT_PLAIN)).toList();

		if (!detectedMediaTypesNotEmptyNotOctetStreamNotPlainText.isEmpty())
			return detectedMediaTypesNotEmptyNotOctetStreamNotPlainText.get(0);

		if (!detectedMediaTypesNotEmptyNotOctetStream.isEmpty())
			return detectedMediaTypesNotEmptyNotOctetStream.get(0);

		return MediaType.OCTET_STREAM;
	}

	private Function<Detector, MediaType> doDetect(InputStream input, Metadata metadata)
	{
		return (detector) -> {
			try
			{
				return detector.detect(input, metadata);
			}
			catch (Exception exception)
			{
				return MediaType.OCTET_STREAM;
			}
		};
	}

	private Predicate<MediaType> notEqualsMediaType(MediaType toCompare)
	{
		return (mediaType) -> toCompare != null && !toCompare.equals(mediaType);
	}
}
