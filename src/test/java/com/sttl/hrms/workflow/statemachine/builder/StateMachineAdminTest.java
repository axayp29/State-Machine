package com.sttl.hrms.workflow.statemachine.builder;

import com.sttl.hrms.workflow.data.Pair;
import com.sttl.hrms.workflow.data.enums.WorkflowType;
import com.sttl.hrms.workflow.data.model.entity.WorkflowTypeEntity;
import com.sttl.hrms.workflow.resource.dto.PassEventDto;
import com.sttl.hrms.workflow.statemachine.util.EventSendHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateMachine;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.sttl.hrms.workflow.data.enums.WorkflowType.LOAN_APPLICATION;
import static com.sttl.hrms.workflow.statemachine.SMConstants.*;
import static com.sttl.hrms.workflow.statemachine.builder.StateMachineBuilder.SMEvent.E_CREATE;
import static com.sttl.hrms.workflow.statemachine.builder.StateMachineBuilder.SMState.S_CLOSED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class StateMachineAdminTest {
    private final StateMachine<String, String> stateMachine;
    private static final Long applicant = 10L;
    private static final Long reviewer1 = 123L;
    private static final Long reviewer2 = 234L;
    private static final Long reviewer3 = 345L;
    private static final Long admin1 = 1L;
    private static final Long admin2 = 2L;
    private static final Long admin3 = 3L;

    public StateMachineAdminTest() throws Exception {
        String stateMachineName = "testStateMachine";
        Map<Integer, Set<Long>> reviewerMap = Map.of(1, Set.of(reviewer1), 2, Set.of(reviewer2), 3,
                Set.of(reviewer3));
        WorkflowTypeEntity.WorkflowProperties wfProps = new WorkflowTypeEntity.WorkflowProperties();
        wfProps.setAdminRoleIds(List.of(admin1, admin2, admin3));
        wfProps.setRollBackApproval(true);
        wfProps.setAdminApproveWorkflow(true);
        wfProps.setParallelApproval(false);
        wfProps.setRollbackMaxCount(3);
        wfProps.setChangeReqMaxCount(3);
        this.stateMachine = StateMachineBuilder.createStateMachine(stateMachineName, reviewerMap, wfProps, applicant);
    }

    @BeforeEach
    void setUp() {
        stateMachine.startReactively().block();
    }

    @AfterEach
    void tearDown() {
        stateMachine.stopReactively().block();
    }

    @RepeatedTest(3)
    void testStateMachineResets() {
        Long wfInstanceId = 0L;
        // create and submit an application for review
        createApplication(wfInstanceId);
        assertEquals(S_CLOSED.name(), stateMachine.getState().getId());
    }

    @Test
    void testAdminCreateAppIsAutoApproved() {
        Long wfInstanceId = 0L;
        ExtendedState extState = stateMachine.getExtendedState();

        // create and submit an application for review
        createApplication(wfInstanceId);

        assertEquals(extState.get(KEY_APPROVE_BY, Pair.class).getSecond(), admin3);
        assertNotNull(extState.get(KEY_APPROVE_COMMENT, String.class));
        assertEquals(S_CLOSED.name(), stateMachine.getState().getId());
        assertEquals(VAL_APPROVED,extState.get(KEY_CLOSED_STATE_TYPE, String.class));
    }



    private List<PassEventDto> createApplication(final Long wfInstanceId) {
        List<PassEventDto> passEvents = createPassEvents(wfInstanceId, LOAN_APPLICATION, admin3, 0, "create " +
                "application", E_CREATE);
        EventSendHelper.passEvents(stateMachine, passEvents);
        return passEvents;
    }

    private static List<PassEventDto> createPassEvents(Long wfId, WorkflowType wfType, Long actionBy, Integer orderNo
            , String comment, StateMachineBuilder.SMEvent... events) {
        List<PassEventDto> passEvents = new ArrayList<>(events.length);
        for (StateMachineBuilder.SMEvent event : events) {
            passEvents.add(PassEventDto.builder()
                    .actionBy(actionBy)
                    .actionDate(LocalDateTime.now())
                    .comment(comment)
                    .event(event.name())
                    .orderNo(orderNo)
                    .workflowInstanceId(wfId)
                    .workflowType(wfType)
                    .build());
        }

        return passEvents;
    }
}
