package de.medizininformatik_initiative.processes.common.activity;

import java.time.Duration;

import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Task;

import de.medizininformatik_initiative.processes.common.util.ConstantsBase;
import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.task.BusinessKeyStrategy;
import dev.dsf.bpe.v2.activity.task.DefaultTaskSender;
import dev.dsf.bpe.v2.activity.values.SendTaskValues;
import dev.dsf.bpe.v2.client.dsf.DelayStrategy;
import dev.dsf.bpe.v2.variables.Variables;

public class RetryTaskSender extends DefaultTaskSender
{
	private final int retryTimes;
	private final Duration retryInterval;

	public RetryTaskSender(ProcessPluginApi api, Variables variables, SendTaskValues sendTaskValues,
			BusinessKeyStrategy businessKeyStrategy)
	{
		this(api, variables, sendTaskValues, businessKeyStrategy, ConstantsBase.DSF_CLIENT_RETRY_6_TIMES,
				ConstantsBase.DSF_CLIENT_RETRY_INTERVAL_5MIN);
	}

	public RetryTaskSender(ProcessPluginApi api, Variables variables, SendTaskValues sendTaskValues,
			BusinessKeyStrategy businessKeyStrategy, int retryTimes, Duration retryInterval)
	{
		super(api, variables, sendTaskValues, businessKeyStrategy);
		this.retryTimes = retryTimes;
		this.retryInterval = retryInterval;
	}

	@Override
	protected IdType doSend(Task task, String targetEndpointUrl)
	{
		return api.getDsfClientProvider().getByEndpointUrl(targetEndpointUrl).withMinimalReturn()
				.withRetry(retryTimes, DelayStrategy.constant(retryInterval)).create(task);
	}
}
