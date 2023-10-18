package com.sttl.hrms.workflow.exception;

/**
 * Use this exception at service layer or higher. For state machine related exceptions, use StateMachineException
 */
public class WorkflowException extends RuntimeException {

    public WorkflowException() {
        super();
    }

    public WorkflowException(String message) {
        super(message);
    }

    public WorkflowException(String message, Throwable cause) {
        super(message, cause);
    }

    public WorkflowException(Throwable cause) {
        super(cause);
    }
}
