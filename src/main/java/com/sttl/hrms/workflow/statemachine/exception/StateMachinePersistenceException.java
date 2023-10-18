package com.sttl.hrms.workflow.statemachine.exception;

import com.sttl.hrms.workflow.exception.WorkflowException;

public class StateMachinePersistenceException extends WorkflowException {

    public StateMachinePersistenceException(String message, Throwable cause) {
        super(message, cause);
    }

}
