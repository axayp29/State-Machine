package com.sttl.hrms.workflow.statemachine.builder;

import com.sttl.hrms.workflow.data.Pair;

import com.sttl.hrms.workflow.data.enums.WorkflowType;
import com.sttl.hrms.workflow.data.model.entity.WorkflowTypeEntity;
import com.sttl.hrms.workflow.resource.dto.PassEventDto;
import com.sttl.hrms.workflow.statemachine.util.EventSendHelper;
import com.sttl.hrms.workflow.statemachine.util.ExtStateUtil;
import org.junit.jupiter.api.*;
import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateMachine;

import java.time.LocalDateTime;
import java.util.*;

import static com.sttl.hrms.workflow.data.enums.WorkflowType.LEAVE_APPLICATION;
import static com.sttl.hrms.workflow.data.enums.WorkflowType.LOAN_APPLICATION;
import static com.sttl.hrms.workflow.statemachine.SMConstants.*;
import static com.sttl.hrms.workflow.statemachine.builder.StateMachineBuilder.SMEvent.*;
import static com.sttl.hrms.workflow.statemachine.builder.StateMachineBuilder.SMState.*;
import static com.sttl.hrms.workflow.statemachine.util.ExtStateUtil.get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class HrmsTest {


    private final StateMachine<String, String> stateMachine;
    private static final Long applicant1 = 10L;
    private static final Long reviewer1 = 13L;
    private static final Long reviewer2 = 137L;
    private static final Long reviewer3 = 4L;
    private static final Long reviewer4 = 170L;
    private static final Long reviewer5 = 123L;
    private static final Long reviewer6 = 678L;
    private static final List<Long> admin1 = new ArrayList<>(Arrays.asList(2L,35L,37L,36L,9L,113L));
   

    public HrmsTest() throws Exception {
        String stateMachineName = "testStateMachine";
        Map<Integer, Set<Long>> reviewerMap = Map.of(1, Set.of(reviewer1), 2, Set.of(reviewer2), 3, Set.of(reviewer3),
        		4,Set.of(reviewer4),5,Set.of(146L,150L,151L,168L,154L,172L));
        WorkflowTypeEntity.WorkflowProperties wfProps = new WorkflowTypeEntity.WorkflowProperties();
        wfProps.setAdminRoleIds(admin1);
        wfProps.setRollBackApproval(true);
        wfProps.setAdminApproveWorkflow(true);
        wfProps.setParallelApproval(true);
        wfProps.setRollbackMaxCount(3);
        wfProps.setChangeReqMaxCount(3);
        this.stateMachine = StateMachineBuilder.createStateMachine(stateMachineName, reviewerMap, wfProps, applicant1);
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
        assertEquals(S_PARALLEL_APPROVAL_FLOW.name(), stateMachine.getState().getId());
    }

    @Test
    void testForwardHappyPath() {
        final ExtendedState extState = stateMachine.getExtendedState();
        Long wfInstanceId = 0L;

        // create and submit an application for review
        createApplication(wfInstanceId);
        assertEquals(S_PARALLEL_APPROVAL_FLOW.name(), stateMachine.getState().getId());

        // first reviewer forwards the application
//        List<PassEventDto> passEvents2 = createPassEvents(wfInstanceId, LEAVE_APPLICATION, reviewer1, 1, "forwarded",
//                E_FORWARD);
//        EventSendHelper.passEvents(stateMachine, passEvents2);
//        assertEquals(1, extState.get(KEY_FORWARDED_COUNT, Integer.class));
//        assertEquals(new Pair<>(1, reviewer1), extState.get(KEY_FORWARDED_BY_LAST, Pair.class));

    }

   

    private List<PassEventDto> createApplication(final Long wfInstanceId) {
        List<PassEventDto> passEvents = createPassEvents(wfInstanceId, LEAVE_APPLICATION, 2L, 0, "create " +
                "application", E_CREATE, E_SUBMIT, E_TRIGGER_REVIEW_OF, E_TRIGGER_FLOW_JUNCTION);
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
