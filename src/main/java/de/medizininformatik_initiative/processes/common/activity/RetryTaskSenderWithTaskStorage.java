package de.medizininformatik_initiative.processes.common.activity;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

import org.hl7.fhir.r4.model.Task;

import de.medizininformatik_initiative.processes.common.error.MessageIntermediateThrowEventErrorHandlerContinuingProcessWithTaskLog;
import de.medizininformatik_initiative.processes.common.error.MessageSendTaskErrorHandlerContinuingProcessWithTaskLog;
import de.medizininformatik_initiative.processes.common.util.ConstantsBase;
import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.task.BusinessKeyStrategy;
import dev.dsf.bpe.v2.activity.values.SendTaskValues;
import dev.dsf.bpe.v2.variables.Target;
import dev.dsf.bpe.v2.variables.Variables;

public class RetryTaskSenderWithTaskStorage extends RetryTaskSender
{
	/**
	 * Should be used with {@link MessageIntermediateThrowEventErrorHandlerContinuingProcessWithTaskLog} or
	 * {@link MessageSendTaskErrorHandlerContinuingProcessWithTaskLog}
	 */
	public RetryTaskSenderWithTaskStorage(ProcessPluginApi api, Variables variables, SendTaskValues sendTaskValues,
			BusinessKeyStrategy businessKeyStrategy,
			Function<Target, List<Task.ParameterComponent>> additionalInputParameters)
	{
		super(api, variables, sendTaskValues, businessKeyStrategy, additionalInputParameters);
	}

	/**
	 * Should be used with {@link MessageIntermediateThrowEventErrorHandlerContinuingProcessWithTaskLog} or
	 * {@link MessageSendTaskErrorHandlerContinuingProcessWithTaskLog}
	 */
	public RetryTaskSenderWithTaskStorage(ProcessPluginApi api, Variables variables, SendTaskValues sendTaskValues,
			BusinessKeyStrategy businessKeyStrategy,
			Function<Target, List<Task.ParameterComponent>> additionalInputParameters, int retryTimes,
			Duration retryInterval)
	{
		super(api, variables, sendTaskValues, businessKeyStrategy, additionalInputParameters, retryTimes,
				retryInterval);
	}

	@Override
	protected TaskAndConfig createTaskAndConfig(BusinessKeyStrategy businessKeyStrategy)
	{
		TaskAndConfig taskAndConfig = super.createTaskAndConfig(businessKeyStrategy);
		Target target = variables.getTarget();
		variables.setFhirResource(
				ConstantsBase.BPMN_EXECUTION_VARIABLE_SENT_TASK + "_" + target.getOrganizationIdentifierValue(),
				taskAndConfig.task());
		return taskAndConfig;
	}
}
