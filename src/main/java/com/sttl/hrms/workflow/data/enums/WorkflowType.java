package com.sttl.hrms.workflow.data.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@RequiredArgsConstructor
@ToString
public enum WorkflowType {
    LEAVE_APPLICATION(1, "LeaveApplicationWorkflow", "leaveapp_wf_status_log"),
    LOAN_APPLICATION(2, "LoanApplicationWorkflow", "loanapp_wf_status_log");

    private final int typeId;

    private final String name;

    private final String tableName;

    private static final WorkflowType[] WORKFLOW_TYPES = WorkflowType.values();

    public static WorkflowType fromId(int typeId) {
        for (WorkflowType workflowType : WORKFLOW_TYPES) {
            if (workflowType.getTypeId() == typeId) {
                return workflowType;
            }
        }
        throw new IllegalArgumentException("No workflowType found for given id: " + typeId);
    }

    public static WorkflowType fromName(String name) {
        for (WorkflowType workflowType : WORKFLOW_TYPES) {
            if (workflowType.getName().equalsIgnoreCase(name)) {
                return workflowType;
            }
        }
        throw new IllegalArgumentException("No workflowType found for given id: " + name);
    }

}
