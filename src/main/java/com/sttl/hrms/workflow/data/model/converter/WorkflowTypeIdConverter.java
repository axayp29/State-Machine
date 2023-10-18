package com.sttl.hrms.workflow.data.model.converter;

import com.sttl.hrms.workflow.data.enums.WorkflowType;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true)
public class WorkflowTypeIdConverter implements AttributeConverter<WorkflowType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(WorkflowType attribute) {
        return attribute.getTypeId();
    }

    @Override
    public WorkflowType convertToEntityAttribute(Integer dbData) {
        return WorkflowType.fromId(dbData);
    }
}
