package com.sttl.hrms.workflow.data.model.converter;

import com.sttl.hrms.workflow.data.enums.WorkflowType;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true)
public class WorkflowTypeNameConverter implements AttributeConverter<WorkflowType, String> {

    @Override
    public String convertToDatabaseColumn(WorkflowType attribute) {
        return attribute.getName();
    }

    @Override
    public WorkflowType convertToEntityAttribute(String dbData) {
        return WorkflowType.fromName(dbData);
    }
}


