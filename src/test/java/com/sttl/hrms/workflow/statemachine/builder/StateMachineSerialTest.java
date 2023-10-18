package com.sttl.hrms.workflow.statemachine.builder;

import com.sttl.hrms.workflow.data.Pair;
import com.sttl.hrms.workflow.data.enums.WorkflowType;
import com.sttl.hrms.workflow.data.model.entity.WorkflowTypeEntity.WorkflowProperties;
import com.sttl.hrms.workflow.resource.dto.PassEventDto;
import com.sttl.hrms.workflow.statemachine.builder.StateMachineBuilder.SMEvent;
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
import static org.junit.jupiter.api.Assertions.*;

class StateMachineSerialTest {

    private final StateMachine<String, String> stateMachine;
    private static final Long applicant1 = 10L;
    private static final Long reviewer1 = 123L;
    private static final Long reviewer2 = 234L;
    private static final Long reviewer3 = 345L;
    private static final Long reviewer4 = 456L;
    private static final Long reviewer5 = 567L;
    private static final Long reviewer6 = 123L;
    private static final Long admin1 = 1L;
    private static final Long admin2 = 2L;
    private static final Long admin3 = 3L;

    public StateMachineSerialTest() throws Exception {
        String stateMachineName = "testStateMachine";
        Map<Integer, Set<Long>> reviewerMap = Map.of(1, Set.of(reviewer1, reviewer4), 2, Set.of(reviewer2,
                reviewer5), 3, Set.of(reviewer3, reviewer6));
        WorkflowProperties wfProps = new WorkflowProperties();
        wfProps.setAdminRoleIds(List.of(admin1, admin2, admin3));
        wfProps.setRollBackApproval(true);
        wfProps.setAdminApproveWorkflow(true);
        wfProps.setParallelApproval(false);
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
        assertEquals(S_SERIAL_APPROVAL_FLOW.name(), stateMachine.getState().getId());
    }

    @Test
    void testForwardHappyPath() {
        final ExtendedState extState = stateMachine.getExtendedState();
        Long wfInstanceId = 0L;

        // create and submit an application for review
        createApplication(wfInstanceId);
        assertEquals(S_SERIAL_APPROVAL_FLOW.name(), stateMachine.getState().getId());

        // first reviewer forwards the application
        List<PassEventDto> passEvents2 = createPassEvents(wfInstanceId, LEAVE_APPLICATION, reviewer1, 1, "forwarded",
                E_FORWARD);
        EventSendHelper.passEvents(stateMachine, passEvents2);
        assertEquals(1, extState.get(KEY_FORWARDED_COUNT, Integer.class));
        assertEquals(new Pair<>(1, reviewer1), extState.get(KEY_FORWARDED_BY_LAST, Pair.class));

        // second reviewer forwards the application
        List<PassEventDto> passEvents3 = createPassEvents(wfInstanceId, LEAVE_APPLICATION, reviewer2, 2, "forwarded",
                E_FORWARD);
        EventSendHelper.passEvents(stateMachine, passEvents3);
        assertEquals(2, extState.get(KEY_FORWARDED_COUNT, Integer.class));
        assertEquals(new Pair<>(2, reviewer2), extState.get(KEY_FORWARDED_BY_LAST, Pair.class));

        // third reviewer forwards (approves) the application
        List<PassEventDto> passEvents4 = createPassEvents(wfInstanceId, LEAVE_APPLICATION, reviewer6, 3, "forwarded",
                E_FORWARD);
        EventSendHelper.passEvents(stateMachine, passEvents4);
        assertEquals(3, extState.get(KEY_FORWARDED_COUNT, Integer.class));
        assertEquals(new Pair<>(3, reviewer6), extState.get(KEY_FORWARDED_BY_LAST, Pair.class));

        // application approved and completed.
        assertEquals(S_CLOSED.name(), stateMachine.getState().getId());
    }

    @Test
    void testCancel() {
        final ExtendedState extState = stateMachine.getExtendedState();
        Long wfInstanceId = 1L;

        createApplication(wfInstanceId);

        List<PassEventDto> passEvents5 = createPassEvents(wfInstanceId, LOAN_APPLICATION, applicant1, 1, "canceled",
                E_CANCEL);
        EventSendHelper.passEvents(stateMachine, passEvents5);
        assertEquals(applicant1, get(extState, KEY_CLOSED_BY, Long.class, null));
        assertEquals(VAL_CANCELED, get(extState, KEY_CLOSED_STATE_TYPE, String.class, ""));
        assertEquals(S_COMPLETED.name(), stateMachine.getState().getId());
    }

    @Nested
    public class RejectTest {
        @Test
        void testLevelOneReject() {
            Long wfInstanceId = 1L;
            ExtendedState extState = stateMachine.getExtendedState();

            // create applicatiton
            createApplication(wfInstanceId);

            // reviewer 1 rejects application
            EventSendHelper.passEvent(stateMachine, PassEventDto.builder()
                    .event(E_REJECT.name())
                    .actionBy(reviewer1)
                    .actionDate(LocalDateTime.now())
                    .workflowType(LOAN_APPLICATION)
                    .workflowInstanceId(wfInstanceId)
                    .orderNo(1)
                    .comment("rejecting application.")
                    .build());

            // test that the application is in closed state because it was rejected
            assertEquals(S_CLOSED.name(), stateMachine.getState().getId());

            // test that reviewer2 was the one who rejected the app
            assertEquals(reviewer1, (get(extState, KEY_REJECTED_BY, Pair.class, null)).getSecond());

            // test that forwarded count is reset
            assertNull(extState.getVariables().get(KEY_FORWARDED_COUNT));
        }

        @Test
        void testLevelTwoReject() {
            Long wfInstanceId = 1L;
            ExtendedState extState = stateMachine.getExtendedState();

            // create application
            createApplication(wfInstanceId);

            // reviewer 1 forwards application
            EventSendHelper.passEvent(stateMachine, PassEventDto.builder()
                    .event(E_FORWARD.name())
                    .actionBy(reviewer1)
                    .actionDate(LocalDateTime.now())
                    .workflowType(LOAN_APPLICATION)
                    .workflowInstanceId(wfInstanceId)
                    .orderNo(1)
                    .comment("forwarding application.")
                    .build());

            // reviewer 2 rejects application
            EventSendHelper.passEvent(stateMachine, PassEventDto.builder()
                    .event(E_REJECT.name())
                    .actionBy(reviewer2)
                    .actionDate(LocalDateTime.now())
                    .workflowType(LOAN_APPLICATION)
                    .workflowInstanceId(wfInstanceId)
                    .orderNo(2)
                    .comment("rejecting application.")
                    .build());

            // test that the application is in closed state because it was rejected
            assertEquals(S_CLOSED.name(), stateMachine.getState().getId());

            // test that reviewer2 was the one who rejected the app
            assertEquals(reviewer2, get(extState, KEY_REJECTED_BY, Pair.class, null).getSecond());

            // test that forwarded count is reset
            assertNull(extState.getVariables().get(KEY_FORWARDED_COUNT));
        }

        @Test
        void testLevelThreeReject() {
            Long wfInstanceId = 1L;
            ExtendedState extState = stateMachine.getExtendedState();

            // create application
            createApplication(wfInstanceId);

            // reviewer 1 forwards application
            EventSendHelper.passEvent(stateMachine, PassEventDto.builder()
                    .event(E_FORWARD.name())
                    .actionBy(reviewer1)
                    .actionDate(LocalDateTime.now())
                    .workflowType(LOAN_APPLICATION)
                    .workflowInstanceId(wfInstanceId)
                    .orderNo(1)
                    .comment("forwarding application.")
                    .build());

            // reviewer 2 forwards application
            EventSendHelper.passEvent(stateMachine, PassEventDto.builder()
                    .event(E_FORWARD.name())
                    .actionBy(reviewer2)
                    .actionDate(LocalDateTime.now())
                    .workflowType(LOAN_APPLICATION)
                    .workflowInstanceId(wfInstanceId)
                    .orderNo(2)
                    .comment("forwarding application.")
                    .build());

            // reviewer 3 rejects application
            EventSendHelper.passEvent(stateMachine, PassEventDto.builder()
                    .event(E_REJECT.name())
                    .actionBy(reviewer6)
                    .actionDate(LocalDateTime.now())
                    .workflowType(LOAN_APPLICATION)
                    .workflowInstanceId(wfInstanceId)
                    .orderNo(3)
                    .comment("rejecting application.")
                    .build());

            // test that the application is in closed state because it was rejected
            assertEquals(S_CLOSED.name(), stateMachine.getState().getId());

            // test that reviewer2 was the one who rejected the app
            assertEquals(reviewer6, get(extState, KEY_REJECTED_BY, Pair.class, null).getSecond());

            // test that forwarded count is reset
            assertNull(extState.getVariables().get(KEY_FORWARDED_COUNT));
        }
    }

    @Nested
    public class RequestChangesTest {
        String requestChangesComment = "please make the following changes";

        @Test
        void requestChangesHappyPath() {
            final ExtendedState extState = stateMachine.getExtendedState();
            Long wfInstanceId = 1L;

            // create and submit application for review
            var passEvents = createApplication(wfInstanceId);
            assertEquals(S_SERIAL_APPROVAL_FLOW.name(), stateMachine.getState().getId());

            // reviewer 1 requests changes
            List<PassEventDto> passEvents2 = createPassEvents(wfInstanceId, LOAN_APPLICATION, reviewer1, 1, requestChangesComment, E_REQUEST_CHANGES_IN);
            EventSendHelper.passEvents(stateMachine, passEvents2);
            assertEquals(0, ExtStateUtil.get(extState, KEY_FORWARDED_COUNT, Integer.class, 0));
            assertEquals(S_CREATED.name(), stateMachine.getState().getId());
            assertEquals(1, extState.get(KEY_RETURN_COUNT, Integer.class));

            // user submits application for review again
            passEvents.removeIf(passEvent -> passEvent.getEvent().equalsIgnoreCase(E_CREATE.name()));
            EventSendHelper.passEvents(stateMachine, passEvents);

            // first reviewer forwards the application
            List<PassEventDto> passEvents3 = createPassEvents(wfInstanceId, LOAN_APPLICATION, reviewer1, 1, "forwarded",
                    E_FORWARD);
            EventSendHelper.passEvents(stateMachine, passEvents3);
            assertEquals(1, extState.get(KEY_FORWARDED_COUNT, Integer.class));
            assertEquals(new Pair<>(1, reviewer1), extState.get(KEY_FORWARDED_BY_LAST, Pair.class));

            // second reviewer forwards the application
            List<PassEventDto> passEvents4 = createPassEvents(wfInstanceId, LOAN_APPLICATION, reviewer2, 2, "forwarded",
                    E_FORWARD);
            EventSendHelper.passEvents(stateMachine, passEvents4);
            assertEquals(2, extState.get(KEY_FORWARDED_COUNT, Integer.class));
            assertEquals(new Pair<>(2, reviewer2), extState.get(KEY_FORWARDED_BY_LAST, Pair.class));

            // third reviewer requests changes
            List<PassEventDto> passEvents5 = createPassEvents(wfInstanceId, LOAN_APPLICATION, reviewer6, 3, "please " +
                            "make changes", E_REQUEST_CHANGES_IN);
            EventSendHelper.passEvents(stateMachine, passEvents5);
            assertEquals(0, extState.get(KEY_FORWARDED_COUNT, Integer.class));
            assertEquals(S_CREATED.name(), stateMachine.getState().getId());
            assertEquals(2, extState.get(KEY_RETURN_COUNT, Integer.class));

        }

        @Test
        void requestChangesLimit() {
            final ExtendedState extState = stateMachine.getExtendedState();
            extState.getVariables().put(KEY_CHANGE_REQ_MAX, 3);
            Long wfInstanceId = 1L;

            // create and submit application for review
            var passEvents = createApplication(wfInstanceId);
            assertEquals(S_SERIAL_APPROVAL_FLOW.name(), stateMachine.getState().getId());

            // reviewer 1 requests changes
            List<PassEventDto> passEvents2 = createPassEvents(wfInstanceId, LOAN_APPLICATION, reviewer1, 1, "please make changes",
                    E_REQUEST_CHANGES_IN);
            EventSendHelper.passEvents(stateMachine, passEvents2);
            assertEquals(0, extState.get(KEY_FORWARDED_COUNT, Integer.class));
            assertEquals(S_CREATED.name(), stateMachine.getState().getId());
            assertEquals(1, extState.get(KEY_RETURN_COUNT, Integer.class));

            // user submits application for review again
            passEvents.removeIf(passEvent -> passEvent.getEvent().equalsIgnoreCase(E_CREATE.name()));
            EventSendHelper.passEvents(stateMachine, passEvents);

            // reviewer 1 requests changes 2nd time
            EventSendHelper.passEvents(stateMachine, passEvents2);
            assertEquals(0, extState.get(KEY_FORWARDED_COUNT, Integer.class));
            assertEquals(S_CREATED.name(), stateMachine.getState().getId());
            assertEquals(2, extState.get(KEY_RETURN_COUNT, Integer.class));

            // user submits application for review again
            EventSendHelper.passEvents(stateMachine, passEvents);

            // reviewer 1 requests changes 3rd time
            EventSendHelper.passEvents(stateMachine, passEvents2);
            assertEquals(0, extState.get(KEY_FORWARDED_COUNT, Integer.class));
            assertEquals(S_CREATED.name(), stateMachine.getState().getId());
            assertEquals(3, extState.get(KEY_RETURN_COUNT, Integer.class));

            // user submits application for review again
            EventSendHelper.passEvents(stateMachine, passEvents);

            // reviewer 1 and reviewer 2 forwards the application
            List<PassEventDto> passEvents3 = createPassEvents(wfInstanceId, LEAVE_APPLICATION, reviewer1, 1, "forwarded",
                    E_FORWARD);
            EventSendHelper.passEvents(stateMachine, passEvents3);
            List<PassEventDto> passEvents4 = createPassEvents(wfInstanceId, LOAN_APPLICATION, reviewer2, 2, "forwarded",
                    E_FORWARD);
            EventSendHelper.passEvents(stateMachine, passEvents4);

            // reviewer 3 requests changes (total, 4th time)
            List<PassEventDto> passEvents5 = createPassEvents(wfInstanceId, LOAN_APPLICATION, reviewer3, 3, "please make " +
                            "changes",
                    E_REQUEST_CHANGES_IN);
            EventSendHelper.passEvents(stateMachine, passEvents5);
            assertEquals(3, extState.get(KEY_RETURN_COUNT, Integer.class));
            assertEquals(2, extState.get(KEY_FORWARDED_COUNT, Integer.class));
            assertEquals(S_SERIAL_APPROVAL_FLOW.name(), stateMachine.getState().getId());
        }

        @Test
        void requestChangesOrder() {
            final ExtendedState extState = stateMachine.getExtendedState();
            Long wfInstanceId = 1L;

            // create and submit application for review
            createApplication(wfInstanceId);
            assertEquals(S_SERIAL_APPROVAL_FLOW.name(), stateMachine.getState().getId());

            // test that if reviewer 2 requests changes then they won't be accepted as its out of reviewer order
            List<PassEventDto> passEvents = createPassEvents(wfInstanceId, LOAN_APPLICATION, reviewer2, 2, "please make " +
                    "changes", E_REQUEST_CHANGES_IN);
            EventSendHelper.passEvents(stateMachine, passEvents);
            assertEquals(0, extState.get(KEY_FORWARDED_COUNT, Integer.class));
            assertEquals(S_SERIAL_APPROVAL_FLOW.name(), stateMachine.getState().getId());
            assertEquals(0, extState.get(KEY_RETURN_COUNT, Integer.class));

            assertTrue(stateMachine.hasStateMachineError());
        }
    }

    @Nested
    public class RollbackTest {

        @SuppressWarnings("unchecked")
        @Test
        void testRollbackFromReject() {

            final ExtendedState extState = stateMachine.getExtendedState();
            Long wfInstanceId = 1L;

            createApplication(wfInstanceId);

            List<PassEventDto> passEvents = createPassEvents(wfInstanceId, LOAN_APPLICATION, reviewer1, 1, "rejected",
                    E_REJECT);
            EventSendHelper.passEvents(stateMachine, passEvents);

            List<PassEventDto> passEvents2 = createPassEvents(wfInstanceId, LOAN_APPLICATION, reviewer4, 1, "rolling " +
                    "back rejection", E_ROLL_BACK);
            EventSendHelper.passEvents(stateMachine, passEvents2);

            assertEquals(1, get(extState, KEY_ROLL_BACK_COUNT, Integer.class, 0));
            Long actualRollBackBy = Optional.ofNullable(get(extState, KEY_ROLL_BACK_BY_LAST, Pair.class, null))
                    .map(pair -> (Pair<Integer, Long>) pair)
                    .map(Pair::getSecond)
                    .orElse(0L);
            assertEquals(reviewer4, actualRollBackBy);
            assertEquals(S_SERIAL_APPROVAL_FLOW.name(), stateMachine.getState().getId());
        }

        @Test
        void testRollbackFromForward() {
            Long wfInstanceId = 1L;

            createApplication(wfInstanceId);

            // reviewer 4 forwards the application
            List<PassEventDto> passEvents = createPassEvents(wfInstanceId, LOAN_APPLICATION, reviewer4, 1, "forward",
                    E_FORWARD);
            EventSendHelper.passEvents(stateMachine, passEvents);

            // reviewer 2 forwards the application
            List<PassEventDto> passEvents2 = createPassEvents(wfInstanceId, LOAN_APPLICATION, reviewer2, 2, "forward",
                    E_FORWARD);
            EventSendHelper.passEvents(stateMachine, passEvents2);

            // reviewer 5 forwrads the application
            List<PassEventDto> passEvents3 = createPassEvents(wfInstanceId, LOAN_APPLICATION, reviewer5, 2, "roll back",
                    E_ROLL_BACK);
            EventSendHelper.passEvents(stateMachine, passEvents3);

            final ExtendedState extState = stateMachine.getExtendedState();
            assertEquals(new Pair<>(1, reviewer4), get(extState, KEY_FORWARDED_BY_LAST, Pair.class, null));
            assertEquals(new Pair<>(2, reviewer5), (get(extState, KEY_ROLL_BACK_BY_LAST, Pair.class, null)));
            assertEquals(S_SERIAL_APPROVAL_FLOW.name(), stateMachine.getState().getId());
            assertEquals(1, get(extState, KEY_ROLL_BACK_COUNT, Integer.class, 0));
        }

        @Test
        void testRollbackFromApprove() {
            Long wfInstanceId = 1L;

            createApplication(wfInstanceId);

            List<PassEventDto> passEvents = createPassEvents(wfInstanceId, LOAN_APPLICATION, reviewer1, 1, "forward",
                    E_FORWARD);
            EventSendHelper.passEvents(stateMachine, passEvents);

            List<PassEventDto> passEvents2 = createPassEvents(wfInstanceId, LOAN_APPLICATION, reviewer2, 2, "forward",
                    E_FORWARD);
            EventSendHelper.passEvents(stateMachine, passEvents2);

            List<PassEventDto> passEvents3 = createPassEvents(wfInstanceId, LOAN_APPLICATION, reviewer6, 3, "forward",
                    E_FORWARD);
            EventSendHelper.passEvents(stateMachine, passEvents3);

            List<PassEventDto> passEvents4 = createPassEvents(wfInstanceId, LOAN_APPLICATION, reviewer6, 3, "roll back",
                    E_ROLL_BACK);
            EventSendHelper.passEvents(stateMachine, passEvents4);

            final ExtendedState extState = stateMachine.getExtendedState();
            assertEquals(new Pair<>(2, reviewer2), get(extState, KEY_FORWARDED_BY_LAST, Pair.class, null));
            assertEquals(new Pair<>(3, reviewer6), get(extState, KEY_ROLL_BACK_BY_LAST, Pair.class, null));
            assertEquals(S_SERIAL_APPROVAL_FLOW.name(), stateMachine.getState().getId());
            assertEquals(1, get(extState, KEY_ROLL_BACK_COUNT, Integer.class, 0));
        }

        @Test
        void testRollbackOrder() {
            Long wfInstanceId = 1L;

            // create application
            createApplication(wfInstanceId);

            // first reviewer forwards application
            List<PassEventDto> passEvents = createPassEvents(wfInstanceId, LOAN_APPLICATION, reviewer1, 1, "forward",
                    E_FORWARD);
            EventSendHelper.passEvents(stateMachine, passEvents);

            // second reviewer forwards application
            List<PassEventDto> passEvents2 = createPassEvents(wfInstanceId, LOAN_APPLICATION, reviewer2, 2, "forward",
                    E_FORWARD);
            EventSendHelper.passEvents(stateMachine, passEvents2);

            // first reviewer tries to roll back forwarding the application
            List<PassEventDto> passEvents3 = createPassEvents(wfInstanceId, LOAN_APPLICATION, reviewer1, 1, "roll back",
                    E_ROLL_BACK);
            EventSendHelper.passEvents(stateMachine, passEvents3);

            assertTrue(stateMachine.hasStateMachineError());

            final ExtendedState extState = stateMachine.getExtendedState();
            assertEquals(new Pair<>(2, reviewer2), get(extState, KEY_FORWARDED_BY_LAST, Pair.class, null));
            assertNull(get(extState, KEY_ROLL_BACK_BY_LAST, Pair.class, null));
            assertEquals(S_SERIAL_APPROVAL_FLOW.name(), stateMachine.getState().getId());
            assertEquals(0, get(extState, KEY_ROLL_BACK_COUNT, Integer.class, 0));
        }
    }

    private List<PassEventDto> createApplication(final Long wfInstanceId) {
        List<PassEventDto> passEvents = createPassEvents(wfInstanceId, LOAN_APPLICATION, applicant1, 0, "create " +
                "application", E_CREATE, E_SUBMIT, E_TRIGGER_REVIEW_OF, E_TRIGGER_FLOW_JUNCTION);
        EventSendHelper.passEvents(stateMachine, passEvents);
        return passEvents;
    }

    private static List<PassEventDto> createPassEvents(Long wfId, WorkflowType wfType, Long actionBy, Integer orderNo
            , String comment, SMEvent... events) {
        List<PassEventDto> passEvents = new ArrayList<>(events.length);
        for (SMEvent event : events) {
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