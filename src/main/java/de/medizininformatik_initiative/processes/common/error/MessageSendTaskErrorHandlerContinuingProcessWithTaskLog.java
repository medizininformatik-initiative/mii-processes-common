package de.medizininformatik_initiative.processes.common.error;

import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.medizininformatik_initiative.processes.common.activity.RetryTaskSenderWithTaskStorage;
import de.medizininformatik_initiative.processes.common.util.ConstantsBase;
import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.values.SendTaskValues;
import dev.dsf.bpe.v2.variables.Variables;

public class MessageSendTaskErrorHandlerContinuingProcessWithTaskLog
		extends MessageSendTaskErrorHandlerContinuingProcess
{
	private static final Logger logger = LoggerFactory
			.getLogger(MessageSendTaskErrorHandlerContinuingProcessWithTaskLog.class);

	/**
	 * Requires {@link RetryTaskSenderWithTaskStorage}
	 */
	public MessageSendTaskErrorHandlerContinuingProcessWithTaskLog()
	{
		super();
	}

	/**
	 * Logs the given {@link Exception}.and the sent FHIR Task ressource
	 * <p>
	 * Returns <code>null</code> to continue process execution.
	 */
	@Override
	public Exception handleException(ProcessPluginApi api, Variables variables, SendTaskValues sendTaskValues,
			Exception exception)
	{
		Exception returnValue = super.handleException(api, variables, sendTaskValues, exception);

		Task task = variables.getFhirResource(ConstantsBase.BPMN_EXECUTION_VARIABLE_SENT_TASK + "_"
				+ variables.getTarget().getOrganizationIdentifierValue());
		logger.error("Task resource for resend of message: {}",
				task != null ? api.getFhirContext().newJsonParser().encodeResourceToString(task) : "null");

		return returnValue;
	}
}
