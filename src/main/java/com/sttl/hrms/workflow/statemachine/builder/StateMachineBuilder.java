package com.sttl.hrms.workflow.statemachine.builder;


import com.sttl.hrms.workflow.data.model.entity.WorkflowTypeEntity.WorkflowProperties;
import com.sttl.hrms.workflow.statemachine.config.StateMachineObserver;
import com.sttl.hrms.workflow.statemachine.config.StateMachineObserver.ExtendedStateListener;
import com.sttl.hrms.workflow.statemachine.config.StateMachineObserver.StateMachineInterceptor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineBuilder.Builder;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.Map;
import java.util.Set;

import static com.sttl.hrms.workflow.statemachine.SMConstants.*;
import static com.sttl.hrms.workflow.statemachine.builder.StateMachineBuilder.SMEvent.*;
import static com.sttl.hrms.workflow.statemachine.builder.StateMachineBuilder.SMState.*;


@Slf4j
public class StateMachineBuilder {

    private StateMachineBuilder() {
        // use class statically
    }

    public static StateMachine<String, String> createStateMachine(String stateMachineName,
            Map<Integer, Set<Long>> reviewerMap, WorkflowProperties wfProps, Long createdBy)
            throws Exception {
        Builder<String, String> builder = org.springframework.statemachine.config.StateMachineBuilder.builder();

        configureStateMachine(builder, stateMachineName);

        configureStates(builder, reviewerMap, wfProps, createdBy);

        configureTransitions(builder, wfProps.isParallelApproval());

        StateMachine<String, String> stateMachine = builder.build();

        stateMachine.getStateMachineAccessor()
                .withAllRegions()
                .forEach(region -> region.addStateMachineInterceptor(new StateMachineInterceptor()));

        stateMachine.getExtendedState().setExtendedStateChangeListener(new ExtendedStateListener());

        return stateMachine;
    }

    private static void configureStateMachine(Builder<String, String> builder, String stateMachineName)
            throws Exception {
        builder.configureConfiguration()
                .withConfiguration()
                    .machineId(stateMachineName)
                    .listener(new StateMachineObserver.StateMachineListener())
                    .autoStartup(true)
                    .and()
                .withMonitoring()
                    .monitor(new StateMachineObserver.StateMachineMonitor());
    }

    private static void configureStates(Builder<String, String> builder, Map<Integer, Set<Long>> reviewerMap,
            WorkflowProperties wfProps, Long createdBy)
            throws Exception {
        builder.configureStates()
                .withStates()
                    .initial(S_INITIAL.name(), context -> Actions.initial(context, wfProps, reviewerMap, createdBy))
//                    .junction(S_CREATE_JUNCTION.name())
                    .states(Set.of(S_CREATED.name(), S_SUBMITTED.name(), S_UNDER_PROCESS.name()))
                    .junction(S_APPROVAL_JUNCTION.name())
                    .state(S_SERIAL_APPROVAL_FLOW.name())
                    .state(S_PARALLEL_APPROVAL_FLOW.name())
                    .state(S_CLOSED.name())
                    .end(S_COMPLETED.name());
    }

    private static void configureTransitions(Builder<String, String> builder, Boolean isParallel) throws Exception {
        StateMachineTransitionConfigurer<String, String> transitions = builder.configureTransitions();

        transitions
                .withExternal().name(TX_USER_CREATES_APP)
                    .source(S_INITIAL.name()).event(E_CREATE.name()).target(S_CREATED.name())
                    .and()

				/*
				 * .withJunction() .source(S_CREATE_JUNCTION.name()) .first(S_CLOSED.name(),
				 * Guards::adminApprove, Actions::approve) .last(S_CREATED.name()).and()
				 */

                .withExternal().name(TX_USER_CANCELS_CREATED_APP)
                    .source(S_CREATED.name()).event(E_CANCEL.name()).target(S_COMPLETED.name())
                    .guard(Guards::cancel).action(Actions::cancel).and()

                .withExternal().name(TX_USER_SUBMITS_APP)
                    .source(S_CREATED.name()).event(E_SUBMIT.name()).target(S_SUBMITTED.name()).and()

                .withExternal().name(TX_SYST_TRGGRS_APP_FOR_REVIEW)
                    .source(S_SUBMITTED.name()).event(E_TRIGGER_REVIEW_OF.name()).target(S_UNDER_PROCESS.name()).and()

                .withExternal().name(TX_SYST_TRGGRS_APPRVL_FLOW)
                    .source(S_UNDER_PROCESS.name()).event(E_TRIGGER_FLOW_JUNCTION.name()).target(S_APPROVAL_JUNCTION.name()).and()

                .withExternal().name(TX_USER_CANCELS_APP_UNDER_REVIEW)
                    .source(S_UNDER_PROCESS.name()).event(E_CANCEL.name()).target(S_COMPLETED.name())
                    .guard(Guards::cancel).action(Actions::cancel).and()

                .withJunction()
                    .source(S_APPROVAL_JUNCTION.name())
                    .first(S_PARALLEL_APPROVAL_FLOW.name(), Guards::approvalFlow).last(S_SERIAL_APPROVAL_FLOW.name());

        if (isParallel)
            configureParallelApprovalTx(transitions);
        else
            configureSerialApprovalTx(transitions);

        transitions
                .withExternal().name(TX_SYST_COMPLETES_APP)
                    .source(S_CLOSED.name()).event(E_TRIGGER_COMPLETE.name()).target(S_COMPLETED.name());
    }

    private static void configureParallelApprovalTx(StateMachineTransitionConfigurer<String, String> transitions)
            throws Exception {
        transitions
                .withExternal().name(TX_USER_CANCELS_APP_PARLL)
                    .source(S_PARALLEL_APPROVAL_FLOW.name()).event(E_CANCEL.name()).target(S_COMPLETED.name())
                    .action(Actions::cancel).and()

                .withExternal().name(TX_RVWR_REJECTS_APP_PARLL)
                    .source(S_PARALLEL_APPROVAL_FLOW.name()).event(E_REJECT.name()).target(S_CLOSED.name())
                    .guard(Guards::reject).action(Actions::reject).and()

                .withInternal().name(TX_RVWR_APPROVES_APP_PARLL)
                    .source(S_PARALLEL_APPROVAL_FLOW.name()).event(E_FORWARD.name())
                    .guard(Guards::forward).action(Actions::forward).and()

                .withExternal().name(TX_RVWR_APPROVES_APP_PARLL)
                    .source(S_PARALLEL_APPROVAL_FLOW.name()).event(E_APPROVE.name()).target(S_CLOSED.name())
                    .guard(Guards::approveInParallel).action(Actions::approve).and()

                .withExternal().name(TX_RVWR_UNDO_APPRVL_PARLL)
                    .source(S_PARALLEL_APPROVAL_FLOW.name()).event(E_ROLL_BACK.name()).target(S_PARALLEL_APPROVAL_FLOW.name())
                    .guard(Guards::rollBackApproval).action(Actions::rollBackApproval).and()

                .withExternal().name(TX_RVWR_UNDO_APPRVL_PARLL)
                    .source(S_CLOSED.name()).event(E_ROLL_BACK.name()).target(S_PARALLEL_APPROVAL_FLOW.name())
                    .guard(Guards::rollBackApproval).action(Actions::rollBackApproval).and()

                .withExternal().name(TX_RVWR_REQ_CHANGES_FRM_USER_PARLL)
                    .source(S_PARALLEL_APPROVAL_FLOW.name()).event(E_REQUEST_CHANGES_IN.name()).target(S_CREATED.name())
                    .guard(Guards::requestChanges).action(Actions::requestChanges);
    }

    private static void configureSerialApprovalTx(StateMachineTransitionConfigurer<String, String> transitions)
            throws Exception {
        transitions
                .withInternal().name(TX_RVWR_FWDS_APP)
                    .source(S_SERIAL_APPROVAL_FLOW.name()).event(E_FORWARD.name())
                    .guard(Guards::forward).action(Actions::forward).and()

                .withExternal().name(TX_RVWR_UNDO_APPRVL_SERIAL)
                    .source(S_SERIAL_APPROVAL_FLOW.name()).event(E_ROLL_BACK.name()).target(S_SERIAL_APPROVAL_FLOW.name())
                    .guard(Guards::rollBackApproval).action(Actions::rollBackApproval).and()

                .withExternal().name(TX_RVWR_UNDO_APPRVL_SERIAL)
                    .source(S_CLOSED.name()).event(E_ROLL_BACK.name()).target(S_SERIAL_APPROVAL_FLOW.name())
                    .guard(Guards::rollBackApproval).action(Actions::rollBackApproval).and()

                .withExternal().name(TX_USER_CANCELS_APP_SERIAL)
                    .source(S_SERIAL_APPROVAL_FLOW.name()).event(E_CANCEL.name()).target(S_COMPLETED.name())
                    .action(Actions::cancel).and()

                .withExternal().name(TX_RVWR_REQ_CHANGES_FRM_USER_SERIAL)
                    .source(S_SERIAL_APPROVAL_FLOW.name()).event(E_REQUEST_CHANGES_IN.name()).target(S_CREATED.name())
                    .guard(Guards::requestChanges).action(Actions::requestChanges).and()

                .withExternal().name(TX_RVWR_REJECTS_APP_SERIAL)
                    .source(S_SERIAL_APPROVAL_FLOW.name()).event(E_REJECT.name()).target(S_CLOSED.name())
                    .guard(Guards::reject).action(Actions::reject).and()

                .withExternal().name(TX_RVWR_APPROVES_APP_SERIAL)
                    .source(S_SERIAL_APPROVAL_FLOW.name()).event(E_APPROVE.name()).target(S_CLOSED.name())
                    .guard(Guards::approveInSerial).action(Actions::approve);
    }

    public enum SMState {
        S_INITIAL,
        S_CREATE_JUNCTION,
        S_CREATED,
        S_SUBMITTED,
        S_UNDER_PROCESS,
        S_CLOSED,
        S_COMPLETED,
        S_APPROVAL_JUNCTION,
        S_SERIAL_APPROVAL_FLOW,
        S_PARALLEL_APPROVAL_FLOW
    }

    @Slf4j
    public enum SMEvent {
        E_CREATE("Create Application"),
        E_SUBMIT("Submit Application"),
        E_TRIGGER_REVIEW_OF("Request Review of Application"),
        E_REQUEST_CHANGES_IN("Request Changes to Submitted Application"),
        E_TRIGGER_FLOW_JUNCTION("Transition to the approval flow type junction"),
        E_FORWARD("Forward Application to the next Approver"),
        E_APPROVE("Approve Application"),
        E_REJECT("Reject Application"),
        E_CANCEL("Cancel Application"),
        E_ROLL_BACK("Roll Back Decision on Application"),
        E_TRIGGER_COMPLETE("Close Application");

        @Getter
        private final String humanReadableStatus;

        private static final SMEvent[] values = SMEvent.values();

        SMEvent(String humanReadableStatus) {
            this.humanReadableStatus = humanReadableStatus;
        }

        public static SMEvent getByName(String name) {
            for (SMEvent e : values) {
                if (e.name().equalsIgnoreCase(name))
                    return e;
            }
            throw new IllegalArgumentException("No event with the given name found");
        }

    }
}

