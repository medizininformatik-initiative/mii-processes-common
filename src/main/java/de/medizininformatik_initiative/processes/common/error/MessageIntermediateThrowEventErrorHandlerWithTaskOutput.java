package de.medizininformatik_initiative.processes.common.error;

import java.util.function.Function;

import org.hl7.fhir.r4.model.Task;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.values.SendTaskValues;
import dev.dsf.bpe.v2.error.impl.DefaultMessageIntermediateThrowEventErrorHandler;
import dev.dsf.bpe.v2.variables.Variables;

public class MessageIntermediateThrowEventErrorHandlerWithTaskOutput
		extends DefaultMessageIntermediateThrowEventErrorHandler
{
	private final Function<Exception, Task.TaskOutputComponent> taskOutputGenerator;

	public MessageIntermediateThrowEventErrorHandlerWithTaskOutput(
			Function<Exception, Task.TaskOutputComponent> taskOutputGenerator)
	{
		this.taskOutputGenerator = taskOutputGenerator;
	}

	@Override
	public Exception handleException(ProcessPluginApi api, Variables variables, SendTaskValues sendTaskValues,
			Exception exception)
	{
		Task task = variables.getStartTask();

		if (task != null)
		{
			task.addOutput(taskOutputGenerator.apply(exception));
			variables.updateTask(task);
		}

		return super.handleException(api, variables, sendTaskValues, exception);
	}
}
