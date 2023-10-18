package com.sttl.hrms.workflow.statemachine.builder;

import com.sttl.hrms.workflow.data.Pair;
import com.sttl.hrms.workflow.data.model.entity.WorkflowTypeEntity.WorkflowProperties;
import com.sttl.hrms.workflow.statemachine.EventResultDto;
import com.sttl.hrms.workflow.statemachine.util.EventSendHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessageHeaders;
import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.state.State;

import java.util.*;
import java.util.stream.Collectors;

import static com.sttl.hrms.workflow.statemachine.SMConstants.*;
import static com.sttl.hrms.workflow.statemachine.builder.StateMachineBuilder.SMEvent.E_APPROVE;
import static com.sttl.hrms.workflow.statemachine.util.ExtStateUtil.get;
import static com.sttl.hrms.workflow.statemachine.util.ExtStateUtil.getStateId;
import static java.util.stream.Collectors.toMap;

/**
 * Order of execution for transition from A -> B: <br>
 * - two states A and B <br>
 * - state entry and exit action on A <br>
 * - state entry and exit action on B <br>
 * - transition action on the A -> B transition <br>
 */
@Slf4j
@SuppressWarnings("unchecked")
public class Actions {

    private Actions() {
        // use class statically
    }

    public static void initial(StateContext<String, String> context, WorkflowProperties workflowProperties,
            Map<Integer, Set<Long>> reviewerMap, Long createdBy) {

        initial(context.getStateMachine(), workflowProperties, reviewerMap, createdBy);
    }

    public static void initial(StateMachine<String, String> stateMachine, WorkflowProperties workflowProperties,
            Map<Integer, Set<Long>> reviewerMap, Long createdBy) {

        String stateId = Optional.ofNullable(stateMachine).flatMap(sm -> Optional.ofNullable(sm.getState())
                .map(State::getId).map(Object::toString)).orElse("null");
        log.trace("Executing action: initialize stateMachine state with currentState: {}", stateId);

        Optional.ofNullable(stateMachine)
                .map(StateMachine::getExtendedState)
                .flatMap(exs -> Optional.ofNullable(exs.getVariables()))
                .ifPresent(stateMap -> setExtendedState(stateMachine, workflowProperties, reviewerMap, stateMap,
                        createdBy));
    }

    private static void setExtendedState(StateMachine<String, String> stateMachine, WorkflowProperties wfProps,
            Map<Integer, Set<Long>> reviewerMap, Map<Object, Object> stateMap, Long createdBy) {
        ExtendedState extState = stateMachine.getExtendedState();

        var workflowProperties = (wfProps == null) ? new WorkflowProperties() : wfProps;

        // property created by
        stateMap.putIfAbsent(KEY_CREATED_BY, createdBy);

        // flow type property
        stateMap.putIfAbsent(KEY_APPROVAL_FLOW_TYPE, Optional.of(workflowProperties)
                .map(WorkflowProperties::isParallelApproval)
                .filter(Boolean::booleanValue).map(flow -> VAL_PARALLEL).orElse(VAL_SERIAL));
        stateMap.putIfAbsent(KEY_ANY_APPROVE, workflowProperties.isSingleApproval());

        // roll back properties
        stateMap.putIfAbsent(KEY_ROLL_BACK_COUNT, 0);
        stateMap.putIfAbsent(KEY_ROLL_BACK_MAX, workflowProperties.getRollbackMaxCount());

        // request change / return properties
        stateMap.putIfAbsent(KEY_RETURN_COUNT, 0);
        stateMap.putIfAbsent(KEY_CHANGE_REQ_MAX, workflowProperties.getChangeReqMaxCount());

        // forward properties
        stateMap.putIfAbsent(KEY_FORWARDED_COUNT, 0);

        // reviwer properties
        stateMap.putIfAbsent(KEY_REVIEWERS_COUNT, reviewerMap.size());
        stateMap.putIfAbsent(KEY_REVIEWERS_MAP, reviewerMap);
        stateMap.putIfAbsent(KEY_FORWARDED_MAP, reviewerMap.entrySet().stream()
                .collect(toMap(Map.Entry::getKey,
                        entry -> entry.getValue().stream().map(userId -> new Pair<>(userId, false)).collect(Collectors.toList()))));

        // admin id property
        stateMap.putIfAbsent(KEY_ADMIN_IDS, workflowProperties.getAdminRoleIds());

        log.trace("Setting extended state- rollbackCountMax: {}, changeRequestsMax: {}, flowType: {}, reviewersCount: {}, "
                        + "reviewersList: {}",
                extState.get(KEY_ROLL_BACK_MAX, Integer.class),
                extState.get(KEY_CHANGE_REQ_MAX, Integer.class),
                extState.get(KEY_APPROVAL_FLOW_TYPE, String.class),
                extState.get(KEY_REVIEWERS_COUNT, Integer.class),
                stateMap.get(KEY_REVIEWERS_MAP));
    }

    public static void forward(StateContext<String, String> context) {
        log.trace("Executing action: forwardStateExitAction with currentState: {}", getStateId(context));

        ExtendedState extState = context.getExtendedState();

        MessageHeaders headers = context.getMessage().getHeaders();
        Long actionBy = get(headers, MSG_KEY_ACTION_BY, Long.class);
        Integer orderNo = get(headers, MSG_KEY_ORDER_NO, Integer.class);
        String comment = get(headers, MSG_KEY_COMMENT, String.class, "");

        // if the userId is an admin, then instead of forwarding, auto-approve the application.
        List<Long> adminIds = get(context, KEY_ADMIN_IDS, List.class, Collections.emptyList());
        boolean adminForwarded = adminIds.contains(actionBy);
        if (adminForwarded) {
            triggerApproveEvent(context);
            return;
        }

        // if a single reviewer is allowed to directly approve the app, then trigger approval.
        // (we have already checked in the guard that the user belongs to the reviewer list)
        boolean anyApprove = get(context, KEY_ANY_APPROVE, Boolean.class, Boolean.FALSE);
        if (anyApprove) {
            triggerApproveEvent(context);
        }

        // 1. set the value in the forward map for given user at specified order
        Map<Integer, List<Pair<Long, Boolean>>> forwardMap = get(extState, KEY_FORWARDED_MAP, Map.class, null);
        forwardMap.forEach((fwdOder, fwdRwrStatusList) -> {
            if (fwdOder.equals(orderNo)) {
                fwdRwrStatusList.forEach(fwdRwrStatusPair -> {
                    if (fwdRwrStatusPair.getFirst().equals(actionBy)) {
                        fwdRwrStatusPair.setSecond(true);
                    }
                });
            }
        });

        // 2. increment the forward count,
        int forwardedCount = get(extState, KEY_FORWARDED_COUNT, Integer.class, 0) + 1;
        int reviewersCount = get(extState, KEY_REVIEWERS_COUNT, Integer.class, 0);
        log.debug("forwardedCount: {}, reviewersCount: {}", forwardedCount, reviewersCount);
        Map<Object, Object> map = extState.getVariables();
        map.put(KEY_FORWARDED_COUNT, forwardedCount);

        // 3. set the last-forward-by record and forward comment
        map.put(KEY_FORWARDED_BY_LAST, new Pair<>(orderNo, actionBy));
        map.put(KEY_FORWARDED_COMMENT, comment);

        // 4. if all reviewers have forwarded the application then approve it
        if (forwardedCount == reviewersCount) {
            triggerApproveEvent(context);
        }
    }

    private static void triggerApproveEvent(StateContext<String, String> context) {
        log.trace("Executing action: autoApproveTransitionAction with currentState: {}", getStateId(context));

        MessageHeaders headers = context.getMessage().getHeaders();
        Map<String, Object> headersMap = new HashMap<>(headers.size());
        headersMap.putAll(headers);

        var result = EventSendHelper.sendMessageToSM(context.getStateMachine(), E_APPROVE.name(), headersMap);

        log.debug("autoApproveTransitionAction results: {}", result
                .stream()
                .map(EventResultDto::toString)
                .collect(Collectors.joining(", ")));
    }

    public static void approve(StateContext<String, String> context) {
        log.trace("Executing action: approveTransitionAction with currentState: {}", getStateId(context));

        MessageHeaders headers = context.getMessage().getHeaders();
        Long actionBy = get(headers, MSG_KEY_ACTION_BY, Long.class, null);
        String comment = get(headers, MSG_KEY_COMMENT, String.class, null);
        Integer orderNo = get(headers, MSG_KEY_ORDER_NO, Integer.class, null);

        ExtendedState extState = context.getExtendedState();
        Map<Object, Object> map = extState.getVariables();

        // add default comment if not present, if the application is approved by admin.
        var adminList = (List<Long>) get(context, KEY_ADMIN_IDS, List.class, Collections.emptyList());
        if (adminList.contains(actionBy)) {
            comment += " (approved by admin)";
        }

        map.put(KEY_APPROVE_BY, new Pair<>(orderNo, actionBy));
        map.put(KEY_APPROVE_COMMENT, comment);
        map.put(KEY_CLOSED_STATE_TYPE, VAL_APPROVED);
        log.trace("Setting extended state- closedState: {}", get(extState, KEY_CLOSED_STATE_TYPE, String.class, ""));
    }

    public static void requestChanges(StateContext<String, String> context) {
        log.trace("Executing action: requestChangesTransitionAction with currentState: {}", getStateId(context));

        MessageHeaders headers = context.getMessage().getHeaders();
        Long actionBy = get(headers, MSG_KEY_ACTION_BY, Long.class, null);
        Integer orderNo = get(headers, MSG_KEY_ORDER_NO, Integer.class, null);
        String comment = get(headers, MSG_KEY_COMMENT, String.class, null);

        ExtendedState extState = context.getExtendedState();
        var map = extState.getVariables();
        map.put(KEY_CHANGES_REQ_BY, new Pair<>(orderNo, actionBy));
        map.put(KEY_CHANGE_REQ_COMMENT, comment);

        // reset counts
        map.put(KEY_RETURN_COUNT, get(extState, KEY_RETURN_COUNT, Integer.class, 0) + 1);
        map.put(KEY_FORWARDED_COUNT, 0);
        map.put(KEY_ROLL_BACK_COUNT, 0);

        log.trace("Setting extended state- returnCount: {}, forwardCount: {}, rollBackCount: {}",
                get(extState, KEY_RETURN_COUNT, Integer.class, null),
                get(extState, KEY_FORWARDED_COUNT, Integer.class, null),
                get(extState, KEY_ROLL_BACK_COUNT, Integer.class, null));
    }

    public static void reject(StateContext<String, String> context) {
        log.trace("Executing action: rejectTransitionAction with currentState: {}", getStateId(context));

        MessageHeaders headers = context.getMessage().getHeaders();
        Long actionBy = get(headers, MSG_KEY_ACTION_BY, Long.class, null);
        String comment = get(headers, MSG_KEY_COMMENT, String.class, null);
        Integer orderNo = get(headers, MSG_KEY_ORDER_NO, Integer.class, null);

        ExtendedState extState = context.getExtendedState();
        var map = extState.getVariables();
        map.put(KEY_REJECTED_BY, new Pair<>(orderNo, actionBy));
        map.put(KEY_REJECTED_COMMENT, comment);
        map.put(KEY_CLOSED_STATE_TYPE, VAL_REJECTED);

        // remove counts in case application rejection is rolled back
        map.remove(KEY_FORWARDED_BY_LAST);
        map.remove(KEY_FORWARDED_COMMENT);
        map.remove(KEY_FORWARDED_COUNT);

        log.trace("Setting extended state- closedState: {}", get(extState, KEY_CLOSED_STATE_TYPE, String.class, ""));
    }

    public static void cancel(StateContext<String, String> context) {
        log.trace("Executing action: cancelTransitionAction with currentState: {}", getStateId(context));

        MessageHeaders headers = context.getMessage().getHeaders();
        Long actionBy = get(headers, MSG_KEY_ACTION_BY, Long.class, null);
        String comment = get(headers, MSG_KEY_COMMENT, String.class, null);

        ExtendedState extState = context.getExtendedState();
        Map<Object, Object> map = extState.getVariables();
        map.put(KEY_CLOSED_STATE_TYPE, VAL_CANCELED);
        map.put(KEY_CLOSED_BY, actionBy);
        map.put(KEY_CLOSED_COMMENT, comment);
        log.trace("Setting extended state- closedState: {}", get(extState, KEY_CLOSED_STATE_TYPE, String.class, ""));
    }

    public static void rollBackApproval(StateContext<String, String> context) {
        log.trace("Executing action: rollBackTransitionAction with currentState: {}", getStateId(context));

        MessageHeaders headers = context.getMessage().getHeaders();
        Long actionBy = get(headers, MSG_KEY_ACTION_BY, Long.class, null);
        Integer orderNo = get(headers, MSG_KEY_ORDER_NO, Integer.class, null);
        String comment = get(headers, MSG_KEY_COMMENT, String.class, null);

        ExtendedState extState = context.getExtendedState();
        Map<Object, Object> map = extState.getVariables();

        map.put(KEY_ROLL_BACK_BY_LAST, new Pair<>(orderNo, actionBy));
        map.put(KEY_ROLL_BACK_COMMENT, comment);

        // increment roll back count
        int currentRollBackCount = get(extState, KEY_ROLL_BACK_COUNT, Integer.class, 0);
        map.put(KEY_ROLL_BACK_COUNT, currentRollBackCount + 1);

        // decrease forward count, don't let forward count be negative.
        int forwardCount = get(extState, KEY_FORWARDED_COUNT, Integer.class, 0);
        map.put(KEY_FORWARDED_COUNT, Math.max((forwardCount - 1), 0));

        // reset closed state
        map.remove(KEY_CLOSED_STATE_TYPE);

        // reset approve by
        map.remove(KEY_APPROVE_BY);

        // reset last entry in forwarded Map
        Map<Integer, List<Pair<Long, Boolean>>> forwardedMap = get(extState, KEY_FORWARDED_MAP, Map.class,
                Collections.emptyMap());
        forwardedMap.entrySet().stream()
                .filter(entry -> entry.getKey().equals(orderNo))
                .flatMap(entry -> entry.getValue().stream())
                .forEach(pair -> pair.setSecond(false));

        // reset forwarded by to previous entry
        forwardedMap.entrySet()
                .stream()
                // only select the entries that are already forwarded
                .filter(entry -> entry.getValue().stream().anyMatch(Pair::getSecond))
                .max(Map.Entry.comparingByKey()) // find the latest entry that is forwarded
                .ifPresentOrElse(e -> e.getValue().stream()
                        .filter(Pair::getSecond)
                        .findFirst()
                        .ifPresent(pair -> map.put(KEY_FORWARDED_BY_LAST, new Pair<>(e.getKey(), pair.getFirst()))),
                        // set the latest  forwarded entry as LAST_FORWARDED_BY
                        () -> map.remove(KEY_FORWARDED_BY_LAST)); // if no entry is present, then remove.

        log.trace("Setting extended state- rollBackCount: {}", get(extState, KEY_ROLL_BACK_COUNT, Integer.class, 0));
    }
}
