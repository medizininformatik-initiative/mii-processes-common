package de.medizininformatik_initiative.processes.common.mimetype;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;

public class NdJsonDetector implements Detector
{
	private static final MediaType MEDIA_TYPE_JSON = MediaType.application("json");
	private static final MediaType MEDIA_TYPE_NDJSON = MediaType.application("x-ndjson");

	private static final int DEFAULT_LINES_TO_CHECK = 3;

	private final Detector defaultDetector;
	private final int linesToCheck;

	public NdJsonDetector(Detector defaultDetector)
	{
		this(defaultDetector, DEFAULT_LINES_TO_CHECK);
	}

	public NdJsonDetector(Detector defaultDetector, int linesToCheck)
	{
		this.defaultDetector = defaultDetector;
		this.linesToCheck = linesToCheck;

		Objects.requireNonNull(this.defaultDetector, "defaultDetector");

		if (this.linesToCheck < 1)
			throw new IllegalArgumentException("lines to check must be greater zero (" + this.linesToCheck + ")");
	}

	/**
	 * Checks the number of defined lines for application/json mimetype.
	 *
	 * @param inputStream
	 *            that should be checked for <code>application/x-ndjson</code>, not <code>null</code>
	 * @param metadata
	 *            will override property <code>Content-Type</code> as checks are line based, not <code>null</code>
	 * @return <code>application/x-ndjson</code> if all lines match <code>application/json</code>,
	 *         <code>application/x-empty</code> otherwise
	 * @throws IOException
	 *             if lines cannot be read from provided inputStream
	 */
	@Override
	public MediaType detect(InputStream inputStream, Metadata metadata) throws IOException
	{
		// Gives only a hint to the possible mime-type, this is needed because application/json
		// cannot be detected without any hint and would resolve to text/plain.
		// As we are checking line by line for JSON to detect if the content is application/x-ndjson, we have to reset
		// the hint to application/json.
		if (MEDIA_TYPE_NDJSON.toString().equals(metadata.get(Metadata.CONTENT_TYPE)))
		{
			metadata.set(Metadata.CONTENT_TYPE, MEDIA_TYPE_JSON.toString());
		}

		List<MediaType> detectedMediaTypes = new ArrayList<>();

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream)))
		{
			String line;
			int lineCounter = 0;

			while ((line = reader.readLine()) != null && lineCounter < linesToCheck)
			{
				InputStream toDetect = new ByteArrayInputStream(line.getBytes());
				MediaType mediaType = defaultDetector.detect(toDetect, metadata);
				detectedMediaTypes.add(mediaType);

				lineCounter++;
			}
		}

		boolean allMatch = detectedMediaTypes.stream().allMatch(this::isJson);

		if (allMatch)
			return MEDIA_TYPE_NDJSON;
		else
			return MediaType.EMPTY;
	}

	private boolean isJson(MediaType mediaType)
	{
		return MEDIA_TYPE_JSON.toString().equals(mediaType.toString());
	}
}
