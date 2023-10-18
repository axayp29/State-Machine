package com.sttl.hrms.workflow.data.model.entity;

import com.sttl.hrms.workflow.data.enums.WorkflowType;
import org.springframework.statemachine.support.DefaultStateMachineContext;


public interface ContextEntity {

    WorkflowType getTypeId();

    String getStateMachineId();

    String getCurrentState();

    DefaultStateMachineContext<String, String> getStateMachineContext();

    void setStateMachineContext(DefaultStateMachineContext<String, String> context);

}