package de.medizininformatik_initiative.processes.common.util;

import java.util.List;
import java.util.Objects;

import org.hl7.fhir.r4.model.BackboneElement;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.ParameterComponent;
import org.hl7.fhir.r4.model.Task.TaskOutputComponent;

public class DataSetStatusGenerator
{
	public ParameterComponent createDataSetStatusInput(String statusVersion, String statusCode, String typeSystem,
			String typeVersion, String typeCode)
	{
		return createDataSetStatusInput(statusVersion, statusCode, typeSystem, typeVersion, typeCode, null);
	}

	public ParameterComponent createDataSetStatusInput(String statusVersion, String statusCode, String typeSystem,
			String typeVersion, String typeCode, String errorMessage)
	{
		ParameterComponent input = new ParameterComponent();
		input.setValue(new Coding().setSystem(ConstantsBase.CODESYSTEM_DATA_SET_STATUS).setVersion(statusVersion)
				.setCode(statusCode));
		input.getType().addCoding().setSystem(typeSystem).setVersion(typeVersion).setCode(typeCode);

		if (errorMessage != null)
			addErrorExtension(input, errorMessage);

		return input;
	}

	public TaskOutputComponent createDataSetStatusOutput(String statusVersion, String statusCode, String typeSystem,
			String typeVersion, String typeCode)
	{
		return createDataSetStatusOutput(statusVersion, statusCode, typeSystem, typeVersion, typeCode, null);
	}

	public TaskOutputComponent createDataSetStatusOutput(String statusVersion, String statusCode, String typeSystem,
			String typeVersion, String typeCode, String errorMessage)
	{
		TaskOutputComponent output = new TaskOutputComponent();
		output.setValue(new Coding().setSystem(ConstantsBase.CODESYSTEM_DATA_SET_STATUS).setVersion(statusVersion)
				.setCode(statusCode));
		output.getType().addCoding().setSystem(typeSystem).setVersion(typeVersion).setCode(typeCode);

		if (errorMessage != null)
			addErrorExtension(output, errorMessage);

		return output;
	}

	private void addErrorExtension(BackboneElement element, String errorMessage)
	{
		element.addExtension().setUrl(ConstantsBase.EXTENSION_DATA_SET_STATUS_ERROR_URL)
				.setValue(new StringType(errorMessage));
	}

	public void transformInputToOutput(Task inputTask, Task outputTask, String typeSystem, String typeVersion,
			String typeCode)
	{
		transformInputToOutputComponents(inputTask, typeSystem, typeVersion, typeCode).forEach(outputTask::addOutput);
	}

	public List<TaskOutputComponent> transformInputToOutputComponents(Task inputTask, String typeSystem,
			String typeVersion, String typeCode)
	{
		Objects.requireNonNull(typeSystem);
		Objects.requireNonNull(typeCode);

		return inputTask.getInput().stream()
				.filter(i -> i.getType().getCoding().stream().anyMatch(c -> typeSystem.equals(c.getSystem())
						&& typeVersion.equals(c.getVersion()) && typeCode.equals(c.getCode())))
				.map(this::toTaskOutputComponent).toList();
	}

	private TaskOutputComponent toTaskOutputComponent(ParameterComponent inputComponent)
	{
		TaskOutputComponent outputComponent = new TaskOutputComponent().setType(inputComponent.getType())
				.setValue(inputComponent.getValue());
		outputComponent.setExtension(inputComponent.getExtension());

		return outputComponent;
	}

	public void transformOutputToInput(Task outputTask, Task inputTask, String typeSystem, String typeVersion,
			String typeCode)
	{
		transformOutputToInputComponent(outputTask, typeSystem, typeVersion, typeCode).forEach(inputTask::addInput);
	}

	public List<ParameterComponent> transformOutputToInputComponent(Task outputTask, String typeSystem,
			String typeVersion, String typeCode)
	{
		Objects.requireNonNull(typeSystem);
		Objects.requireNonNull(typeCode);

		return outputTask.getOutput().stream()
				.filter(i -> i.getType().getCoding().stream().anyMatch(c -> typeSystem.equals(c.getSystem())
						&& typeVersion.equals(c.getVersion()) && typeCode.equals(c.getCode())))
				.map(this::toTaskInputComponent).toList();
	}

	private ParameterComponent toTaskInputComponent(TaskOutputComponent outputComponent)
	{
		ParameterComponent inputComponent = new ParameterComponent().setType(outputComponent.getType())
				.setValue(outputComponent.getValue());
		inputComponent.setExtension(outputComponent.getExtension());

		return inputComponent;
	}
}
