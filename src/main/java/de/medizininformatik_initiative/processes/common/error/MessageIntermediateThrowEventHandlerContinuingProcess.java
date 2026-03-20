package de.medizininformatik_initiative.processes.common.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.values.SendTaskValues;
import dev.dsf.bpe.v2.error.MessageIntermediateThrowEventErrorHandler;
import dev.dsf.bpe.v2.variables.Variables;

public class MessageIntermediateThrowEventHandlerContinuingProcess implements MessageIntermediateThrowEventErrorHandler
{
	private static final Logger logger = LoggerFactory
			.getLogger(MessageIntermediateThrowEventHandlerContinuingProcess.class);

	/**
	 * Only logs the given {@link Exception}.
	 * <p>
	 * Returns <code>null</code> to continue process execution.
	 */
	@Override
	public Exception handleException(ProcessPluginApi api, Variables variables, SendTaskValues sendTaskValues,
			Exception exception)
	{
		logger.debug("Error while executing Task message send {}", getClass().getName(), exception);
		logger.error("Process {} has non-fatal error (continuing execution) in step {} for Task {}, reason: {} - {}",
				variables.getProcessDefinitionId(), variables.getActivityInstanceId(),
				api.getTaskHelper().getLocalVersionlessAbsoluteUrl(variables.getStartTask()),
				exception.getClass().getName(), exception.getMessage());

		return null;
	}
}
