package com.sttl.hrms.workflow.data.enums;

import com.sttl.hrms.workflow.data.Pair;
import com.sttl.hrms.workflow.exception.WorkflowException;
import lombok.Getter;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.sttl.hrms.workflow.statemachine.SMConstants.LEAVE_APP_WF_V1;
import static com.sttl.hrms.workflow.statemachine.SMConstants.LOAN_APP_WF_V1;

@Getter
public enum WorkFlowTypeStateMachine {
    LEAVE_APP_SM(WorkflowType.LEAVE_APPLICATION, List.of(new Pair<>("v1", LEAVE_APP_WF_V1))),
    LOAN_APP_SM(WorkflowType.LOAN_APPLICATION, List.of(new Pair<>("v1", LOAN_APP_WF_V1)));

    private final WorkflowType workflowType;
    private final List<Pair<String, String>> stateMachineIds;
    private static final WorkFlowTypeStateMachine[] values = WorkFlowTypeStateMachine.values();

    WorkFlowTypeStateMachine(WorkflowType workflowType, List<Pair<String, String>> stateMachineIds) {
        this.workflowType = workflowType;
        this.stateMachineIds = stateMachineIds;
    }

    public static List<Pair<String, String>> getSMIdsFromWFType(WorkflowType workflowType) {
        for (WorkFlowTypeStateMachine wfsm : values) {
            if (wfsm.getWorkflowType().equals(workflowType))
                return wfsm.getStateMachineIds();
        }
        return Collections.emptyList();
    }

    public static String getLatestSMIdFromWFType(WorkflowType workflowType) {
        return getSMIdsFromWFType(workflowType)
                .stream()
                .max(Comparator.comparing(Pair::getFirst))
                .map(Pair::getSecond)
                .orElseThrow(() -> new WorkflowException("No statemachine id found for given workflowType" + workflowType));
    }
}
