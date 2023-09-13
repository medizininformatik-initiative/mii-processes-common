package de.medizininformatik_initiative.processes.common.util;

import java.util.Objects;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.BackboneElement;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.ParameterComponent;
import org.hl7.fhir.r4.model.Task.TaskOutputComponent;

public class DataSetStatusGenerator
{
	public ParameterComponent createDataSetStatusInput(String statusCode, String typeSystem, String typeCode)
	{
		return createDataSetStatusInput(statusCode, typeSystem, typeCode, null);
	}

	public ParameterComponent createDataSetStatusInput(String statusCode, String typeSystem, String typeCode,
			String errorMessage)
	{
		ParameterComponent input = new ParameterComponent();
		input.setValue(new Coding().setSystem(ConstantsBase.CODESYSTEM_DATA_SET_STATUS).setCode(statusCode));
		input.getType().addCoding().setSystem(typeSystem).setCode(typeCode);

		if (errorMessage != null)
			addErrorExtension(input, errorMessage);

		return input;
	}

	public TaskOutputComponent createDataSetStatusOutput(String statusCode, String typeSystem, String typeCode)
	{
		return createDataSetStatusOutput(statusCode, typeSystem, typeCode, null);
	}

	public TaskOutputComponent createDataSetStatusOutput(String statusCode, String typeSystem, String typeCode,
			String errorMessage)
	{
		TaskOutputComponent output = new TaskOutputComponent();
		output.setValue(new Coding().setSystem(ConstantsBase.CODESYSTEM_DATA_SET_STATUS).setCode(statusCode));
		output.getType().addCoding().setSystem(typeSystem).setCode(typeCode);

		if (errorMessage != null)
			addErrorExtension(output, errorMessage);

		return output;
	}

	private void addErrorExtension(BackboneElement element, String errorMessage)
	{
		element.addExtension().setUrl(ConstantsBase.EXTENSION_DATA_SET_STATUS_ERROR_URL)
				.setValue(new StringType(errorMessage));
	}

	public void transformInputToOutput(Task inputTask, Task outputTask, String typeSystem, String typeCode)
	{
		transformInputToOutputComponents(inputTask, typeSystem, typeCode).forEach(outputTask::addOutput);
	}

	public Stream<TaskOutputComponent> transformInputToOutputComponents(Task inputTask, String typeSystem,
			String typeCode)
	{
		Objects.requireNonNull(typeSystem);
		Objects.requireNonNull(typeCode);

		return inputTask.getInput().stream()
				.filter(i -> i.getType().getCoding().stream()
						.anyMatch(c -> typeSystem.equals(c.getSystem()) && typeCode.equals(c.getCode())))
				.map(this::toTaskOutputComponent);
	}

	private TaskOutputComponent toTaskOutputComponent(ParameterComponent inputComponent)
	{
		TaskOutputComponent outputComponent = new TaskOutputComponent().setType(inputComponent.getType())
				.setValue(inputComponent.getValue().copy());
		outputComponent.setExtension(inputComponent.getExtension());

		return outputComponent;
	}

	public void transformOutputToInput(Task outputTask, Task inputTask, String typeSystem, String typeCode)
	{
		transformOutputToInputComponent(outputTask, typeSystem, typeCode).forEach(inputTask::addInput);
	}

	public Stream<ParameterComponent> transformOutputToInputComponent(Task outputTask, String typeSystem,
			String typeCode)
	{
		Objects.requireNonNull(typeSystem);
		Objects.requireNonNull(typeCode);

		return outputTask.getOutput().stream()
				.filter(i -> i.getType().getCoding().stream()
						.anyMatch(c -> typeSystem.equals(c.getSystem()) && typeCode.equals(c.getCode())))
				.map(this::toTaskInputComponent);
	}

	private ParameterComponent toTaskInputComponent(TaskOutputComponent outputComponent)
	{
		ParameterComponent inputComponent = new ParameterComponent().setType(outputComponent.getType())
				.setValue(outputComponent.getValue().copy());
		inputComponent.setExtension(outputComponent.getExtension());

		return inputComponent;
	}
}
