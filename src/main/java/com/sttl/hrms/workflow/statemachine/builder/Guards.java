package com.sttl.hrms.workflow.statemachine.builder;


import com.sttl.hrms.workflow.data.Pair;
import com.sttl.hrms.workflow.data.model.entity.WorkflowTypeEntity.WorkflowProperties;
import com.sttl.hrms.workflow.statemachine.exception.StateMachineException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessageHeaders;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;

import java.util.*;
import java.util.stream.Collectors;

import static com.sttl.hrms.workflow.statemachine.SMConstants.*;
import static com.sttl.hrms.workflow.statemachine.builder.StateMachineBuilder.SMState.S_CLOSED;
import static com.sttl.hrms.workflow.statemachine.util.ExtStateUtil.get;
import static com.sttl.hrms.workflow.statemachine.util.ExtStateUtil.getStateId;

@Slf4j
@SuppressWarnings("unchecked")
public class Guards {

    private Guards() {
        // use class statically
    }

    private static final WorkflowProperties defaultWFP = new WorkflowProperties();

    // check whether the approval flow is serial or parallel
    public static boolean approvalFlow(StateContext<String, String> context) {
        log.debug("Executing guard: approvalFlowGuard with currentState: {}", getStateId(context));
        String approvalFlow = get(context, KEY_APPROVAL_FLOW_TYPE, String.class, VAL_SERIAL);
        return approvalFlow.equalsIgnoreCase(VAL_PARALLEL);
    }

    public static boolean approveInParallel(StateContext<String, String> context) {
        log.debug("Executing guard: parallelApprovalGuard with currentState: {}", getStateId(context));

        MessageHeaders headers = context.getMessage().getHeaders();
        Long actionBy = get(headers, MSG_KEY_ACTION_BY, Long.class, null);

        if (adminApprove(context)) return true;

        // check that the approving reviewer is present in the list of reviewers for the application
        Map<Integer, Set<Long>> reviewerMap = (Map<Integer, Set<Long>>) get(context, KEY_REVIEWERS_MAP, Map.class,
                Collections.emptyMap());
        Set<Long> reviewerList = reviewerMap.values().stream().flatMap(Set::stream).collect(Collectors.toSet());
        if (isUserAbsentFromUserList(context.getStateMachine(), reviewerList, actionBy, "reviewer",
                "approve")) return false;

        boolean anyApprove = get(context, KEY_ANY_APPROVE, Boolean.class, Boolean.FALSE);
        if (anyApprove)
            return true;

        int forwardedCount = get(context.getExtendedState(), KEY_FORWARDED_COUNT, Integer.class, 0);
        int reviewersCount = get(context.getExtendedState(), KEY_REVIEWERS_COUNT, Integer.class, 0);
        if (forwardedCount != reviewersCount)
            return false;

        return true;
    }

    public static boolean approveInSerial(StateContext<String, String> context) {
        log.debug("Executing guard: serialApprovalGuard with currentState: {}", getStateId(context));

        if (adminApprove(context)) return true;

        // check that the number of times the application is forwarded is equal to the total number of reviewers.
        int totalReviewers = get(context, KEY_REVIEWERS_COUNT, Integer.class, 0);
        int forwardedTimes = get(context, KEY_FORWARDED_COUNT, Integer.class, 0);
        boolean forwardedCountMatchesTotalReviewers = totalReviewers == forwardedTimes;
        if (!forwardedCountMatchesTotalReviewers) return false;

        // check that all the reviewers have forwarded the application
        Map<Integer, List<Pair<Long, Boolean>>> forwardedMap = (Map<Integer, List<Pair<Long, Boolean>>>) get(context,
                KEY_FORWARDED_MAP, Map.class, Collections.emptyMap());
        boolean allReviewersHaveApproved = forwardedMap.entrySet().stream()
                .allMatch(entry -> entry.getValue().stream().anyMatch(Pair::getSecond));
        if (!allReviewersHaveApproved) return false;

        // check that the last reviewer in the order is the one who forwarded the application
        Pair<Integer, Long> forwardedBy = (Pair<Integer, Long>) get(context, KEY_FORWARDED_BY_LAST, Pair.class, null);
        if (forwardedBy != null && forwardedBy.getFirst() != null && forwardedBy.getSecond() != null) {
            Map<Integer, Set<Long>> reviewerMap = (Map<Integer, Set<Long>>) get(context, KEY_REVIEWERS_MAP, Map.class,
                    Collections.emptyMap());
            return reviewerMap.entrySet().stream()
                    .max(Map.Entry.comparingByKey())
                    .filter(entry -> forwardedBy.getFirst().equals(entry.getKey()) &&
                            entry.getValue().contains(forwardedBy.getSecond()))
                    .isPresent();
        }

        return true;
    }

    public static boolean adminApprove(StateContext<String, String> context) {

        MessageHeaders headers = context.getMessage().getHeaders();
        Long actionBy = get(headers, MSG_KEY_ACTION_BY, Long.class, null);

        // check that the approving admin is valid (i.e. not null or 0)
        if (isUserIdInvalid(context.getStateMachine(), actionBy, "admin-approve")) return false;

        // check that the admin user id is in admin list
        var adminList = (List<Long>) get(context, KEY_ADMIN_IDS, List.class, Collections.emptyList());
        return adminList.contains(actionBy);
    }

    public static boolean requestChanges(StateContext<String, String> context) {

        MessageHeaders headers = context.getMessage().getHeaders();
        Long actionBy = get(headers, MSG_KEY_ACTION_BY, Long.class, null);
        Integer orderNo = get(headers, MSG_KEY_ORDER_NO, Integer.class, 0);
        String comment = get(headers, MSG_KEY_COMMENT, String.class, null);

        // check that the reviewer requesting changes is valid
        if (isUserIdInvalid(context.getStateMachine(), actionBy, "requestChanges")) return false;

        // check that the reviewer belongs to the list of reviewers for the application.
        var reviewerMap = (Map<Integer, Set<Long>>) get(context, KEY_REVIEWERS_MAP, Map.class, Collections.emptyMap());
        Set<Long> reviewerList = reviewerMap.values().stream().flatMap(Set::stream).collect(Collectors.toSet());
        if (isUserAbsentFromUserList(context.getStateMachine(), reviewerList, actionBy, "reviewers",
                "requestChanges"))
            return false;

        // check that the reviwer requesting changes has input a valid comment for the request
        if (isCommentInvalid(context.getStateMachine(), comment, "requestChanges")) return false;

        // check that the maximum allowed rollbacks is not 0, and that total number of returns so far don't exceed its max threshold.
        int maxAllowedReturns = get(context, KEY_CHANGE_REQ_MAX, Integer.class, defaultWFP.getChangeReqMaxCount());
        int returnsSoFar = get(context, KEY_RETURN_COUNT, Integer.class, 0);
        if (isCountExceedingThreshold(context.getStateMachine(), returnsSoFar, maxAllowedReturns, "Change Requests", "requestChanges"))
            return false;

        Pair<Integer, Long> lastForwardedBy = get(context, KEY_FORWARDED_BY_LAST, Pair.class, null);

        boolean isSerialFlow = get(context, KEY_APPROVAL_FLOW_TYPE, String.class, VAL_SERIAL).equalsIgnoreCase(VAL_SERIAL);
        if (isSerialFlow) {
            // if the application has been forwarded before:
            if (lastForwardedBy != null && lastForwardedBy.getSecond() != null) {
                // check that the current user is the last person to have forwarded the application,
                // i.e., no one has forwarded the application after the current user.
                Map<Integer, List<Pair<Long, Boolean>>> forwardMap = get(context, KEY_FORWARDED_MAP, Map.class,
                        Collections.emptyMap());
                var entrySet = new HashSet<>(forwardMap.entrySet());
                entrySet.removeIf(entry -> entry.getValue().stream().anyMatch(Pair::getSecond)); // (remove all
                // successfully forwarded entries)
                for (var entry : entrySet) {
                    // (check if any orderNo is higher than the current user for the remaining entries.)
                    if (entry.getKey() > orderNo) {
                        String errorMsg =
                                "Guard failed for requestChanges as the reviewer: " + actionBy + " at position: " + orderNo + " cannot request" +
                                        " changes in the application as there are other reviewers who have already forwarded the application after them";
                        context.getStateMachine().setStateMachineError(new StateMachineException(errorMsg));
                        return false;
                    }
                }
            } else { // if the application has not been forwarded before:
                // check that the current user is the first order user in the reviewerMap
                if (!reviewerMap.get(1).contains(actionBy)) {
                    String errorMsg =
                            "Guard failed for requestChanges as the reviewer: " + actionBy + " at position: " + orderNo +
                                    " cannot request changes in the application as they are not present in the reviewer list";
                    context.getStateMachine().setStateMachineError(new StateMachineException(errorMsg));
                    return false;
                }
            }
        }

        return true;
    }

    public static boolean rollBackApproval(StateContext<String, String> context) {
        log.debug("Executing guard: rollBackCountGuard with currentState: {}", getStateId(context));

        MessageHeaders headers = context.getMessage().getHeaders();
        Long actionBy = get(headers, MSG_KEY_ACTION_BY, Long.class, null);
        String comment = get(headers, MSG_KEY_COMMENT, String.class, null);
        Integer orderNo = get(headers, MSG_KEY_ORDER_NO, Integer.class, null);

        // check that userId is valid
        if (isUserIdInvalid(context.getStateMachine(), actionBy, "rollBack")) return false;

        // check that we have not hit the max rollback limit
        int rollbackCount = get(context, KEY_ROLL_BACK_COUNT, Integer.class, 0);
        int maxRollBack = get(context, KEY_ROLL_BACK_MAX, Integer.class, defaultWFP.getRollbackMaxCount());
        if (isCountExceedingThreshold(context.getStateMachine(), rollbackCount, maxRollBack, "rollBackMax", "rollBack"))
            return false;

        // check that comment is valid
        if (isCommentInvalid(context.getStateMachine(), comment, "rollback Approval")) return false;

        // check that the application has been forwarded / approved or rejected - else there is nothing to roll back.
        Pair<Integer, Long> forwardBy = ((Pair<Integer, Long>) get(context, KEY_FORWARDED_BY_LAST, Pair.class,
                null));
        Pair<Integer, Long> rejectedBy = (Pair<Integer, Long>) get(context, KEY_REJECTED_BY, Pair.class, null);
        String currentState = context.getStateMachine().getState().getId();
        if (forwardBy == null && rejectedBy == null && currentState.equalsIgnoreCase(S_CLOSED.name())) {
            String errorMsg =
                    "Guard Failed: Cannot roll back the application as the application hasn't been forwarded or " +
                            "rejected or approved";
            context.getStateMachine().setStateMachineError(new StateMachineException(errorMsg));
            return false;
        }

        // check that the user requesting roll back is in the admin list
        var adminList = (List<Long>) get(context, KEY_ADMIN_IDS, List.class, Collections.emptyList());
        if (adminList.contains(actionBy)) {
            return true;
        }

        // check that the user requesting rollback is in the reviewer list
        var reviewerMap = (Map<Integer, Set<Long>>) get(context, KEY_REVIEWERS_MAP, Map.class, Collections.emptyMap());
        Set<Long> reviewerList = reviewerMap.values().stream().flatMap(Set::stream).collect(Collectors.toSet());
        if (isUserAbsentFromUserList(context.getStateMachine(), reviewerList, actionBy, "reviewers", "rollBack"))
            return false;

        // check that for serial approval flow, the user rolling back approval is the latest reviewer who forwarded the application
        boolean isSerial = get(context, KEY_APPROVAL_FLOW_TYPE, String.class, VAL_SERIAL).equalsIgnoreCase(VAL_SERIAL);
        if (isSerial) {
            Map<Integer, List<Pair<Long, Boolean>>> forwardMap = get(context, KEY_FORWARDED_MAP, Map.class,
                    Collections.emptyMap());
            boolean userPresent = forwardMap.entrySet().stream()
                    .filter(entry -> entry.getKey().equals(orderNo))
                    .map(Map.Entry::getValue)
                    .anyMatch(pairList -> {
                        boolean isPresent = pairList.stream().map(Pair::getFirst).anyMatch(actionBy::equals);
                        if (forwardBy != null)
                            return isPresent && pairList.stream().map(Pair::getFirst).anyMatch(forwardBy.getSecond()::equals);
                        return isPresent;
                    });
            if (!userPresent) {
                String errorMsg =
                        "Guard Failed: Cannot roll back the application as the roll back reviewerId: " + actionBy +
                                " does not match the forwarded reviewerId: " + forwardBy.getSecond();
                context.getStateMachine().setStateMachineError(new StateMachineException(errorMsg));
                return false;
            }
        }

        return true;
    }

    public static boolean forward(StateContext<String, String> context) {

        MessageHeaders headers = context.getMessage().getHeaders();
        Long actionBy = get(headers, MSG_KEY_ACTION_BY, Long.class, null);
        Integer orderNo = get(headers, MSG_KEY_ORDER_NO, Integer.class, null);
        String comment = get(headers, MSG_KEY_COMMENT, String.class, null);

        var adminIds = (List<Long>) get(context, KEY_ADMIN_IDS, List.class, Collections.emptyList());

        // check if admin action, then always allow.
        if (adminIds.contains(actionBy)) return true;

        // check if the application is forwarded more times than the number of reviewers.
        if (!reviewCountCheck(context)) return false;

        // check that the reviewer forwarding the application is valid (i.e. not null or 0)
        if (isUserIdInvalid(context.getStateMachine(), actionBy, "forward")) return false;

        // check that the comment is not null or empty.
        if (isCommentInvalid(context.getStateMachine(), comment, "forward")) return false;

        Map<Integer, Set<Long>> reviewerMap = (Map<Integer, Set<Long>>) get(context, KEY_REVIEWERS_MAP, Map.class,
                Collections.emptyMap());

        // check that the forwardingId is present in the reviewerMap
        Set<Long> reviewerList = reviewerMap.values().stream().flatMap(Set::stream).collect(Collectors.toSet());
        if (isUserAbsentFromUserList(context.getStateMachine(), reviewerList, actionBy, "reviewers",
                "forward"))
            return false;

        // check that another reviwer at the same role / order level hasn't already forwarded the application.
        Map<Integer, List<Pair<Long, Boolean>>> forwardMap = (Map<Integer, List<Pair<Long, Boolean>>>) get(context,
                KEY_FORWARDED_MAP, Map.class, Collections.emptyMap());
        boolean alreadyForwardedAtSameLevel = forwardMap.entrySet().stream()
                .filter(entry -> entry.getKey().equals(orderNo))
                .anyMatch(entry -> entry.getValue().stream().anyMatch(Pair::getSecond));
        if (alreadyForwardedAtSameLevel) {
            String errorMsg = "Guard Failed for: " + "forward" + " as at the forwarding order " + orderNo +
                    " another reviwer has already forwarded this application";
            context.getStateMachine().setStateMachineError(new StateMachineException(errorMsg));
            return false;
        }

        boolean isSerialFlow = get(context, KEY_APPROVAL_FLOW_TYPE, String.class, VAL_SERIAL).equalsIgnoreCase(VAL_SERIAL);
        if (isSerialFlow) {
            return forwardSerialCheck(context, actionBy, orderNo);
        } else
            return forwardParallelCheck(context, actionBy, orderNo);
    }

    private static boolean forwardParallelCheck(StateContext<String, String> context, Long forwardingId,
            Integer forwardingOrder) {
        Map<Integer, Set<Long>> reviewerMap = (Map<Integer, Set<Long>>) get(context, KEY_REVIEWERS_MAP, Map.class,
                Collections.emptyMap());

        // check that the order number and the userId of the forwarding reviewer are present in the list of reviewers for the application.
        boolean isOrderNumberAndReviewerIdAbsent = reviewerMap.entrySet()
                .stream()
                .noneMatch(entry ->  entry.getValue().contains(forwardingId));

        if (isOrderNumberAndReviewerIdAbsent) {
            String errorMsg = "Guard Failed for: " + "forward" + " as the combination of the forwarding order " +
                    "and the forwarding userId is not present in the list of reviewers";
            context.getStateMachine().setStateMachineError(new StateMachineException(errorMsg));
            return false;
        }

        Map<Integer, List<Pair<Long, Boolean>>> forwardMap = (Map<Integer, List<Pair<Long, Boolean>>>) get(context,
                KEY_FORWARDED_MAP, Map.class, Collections.emptyMap());

        boolean forwardedAlready = forwardMap.entrySet()
                .stream()
                .filter(entry -> entry.getKey().equals(forwardingOrder))
                .flatMap(entry -> entry.getValue().stream())
                .anyMatch(Pair::getSecond);

        // check that
        // 1. the user is present in forwardingMap, and
        // 2. the same reviewer hasn't already forwarded this application before.
        if (forwardedAlready) {
            String errorMsg = "Guard Failed for: " + "forward" + " either the user has forwarded the application " +
                    "before, or this user id is not present in the forwarding list";
            context.getStateMachine().setStateMachineError(new StateMachineException(errorMsg));
            return false;
        }

        return true;
    }

    private static boolean forwardSerialCheck(StateContext<String, String> context, Long forwardingId, Integer forwardingOrder) {
        Map<Integer, Set<Long>> reviewerMap = (Map<Integer, Set<Long>>) get(context, KEY_REVIEWERS_MAP, Map.class,
                Collections.emptyMap());

        // check that the order number and the userId of the forwarding reviewer are present in the list of reviewers for the application.
        boolean isOrderNumberAndReviewerIdPresent = reviewerMap
                .entrySet()
                .stream()
                .anyMatch(entry -> entry.getKey().equals(forwardingOrder) && entry.getValue().contains(forwardingId));

        if (!isOrderNumberAndReviewerIdPresent) {
            String errorMsg =
                    "Guard Failed for: " + "forward" + " as the combination of the forwarding order " + forwardingOrder +
                    "and the forwarding userId: " + forwardingId + " is not present in the list of reviewers";
            context.getStateMachine().setStateMachineError(new StateMachineException(errorMsg));
            return false;
        }

        Map<Integer, List<Pair<Long, Boolean>>> forwardMap = (Map<Integer, List<Pair<Long, Boolean>>>) get(context,
                KEY_FORWARDED_MAP, Map.class, Collections.emptyMap());

        Optional<Integer> forwardEntryIndex = forwardMap.entrySet().stream()
                .filter(entry -> entry.getKey().equals(forwardingOrder) &&
                        entry.getValue().stream()
                                .anyMatch(pair -> pair.getFirst().equals(forwardingId) && !pair.getSecond()))
                .map(Map.Entry::getKey)
                .findFirst();

        // check that
        // 1. the user is present in forwardingMap, and
        // 2. the same reviewer hasn't already forwarded this application before.
        if (forwardEntryIndex.isEmpty()) {
            String errorMsg = "Guard Failed for: " + "forward" + " either the user has forwarded the application " +
                    "before, or this user id is not present in the forwarding list";
            context.getStateMachine().setStateMachineError(new StateMachineException(errorMsg));
            return false;
        }

        // if first entry, then no need to check entries before it.
        if (forwardEntryIndex.get() == 1)
            return true;

        // check that all reviewers before the current one have already forwarded this application
        boolean isforwardingOrderMaintained = forwardMap.entrySet().stream()
                .filter(entry -> entry.getKey() < forwardEntryIndex.get())
                .allMatch(entry -> entry.getValue().stream().anyMatch(Pair::getSecond));
        if (!isforwardingOrderMaintained) {
            String errorMsg = "Guard Failed for: " + "forward" + " as previous reviewers have not forwarded the " +
                    "application";
            context.getStateMachine().setStateMachineError(new StateMachineException(errorMsg));
            return false;
        }

        return true;
    }

    public static boolean reject(StateContext<String, String> context) {
        MessageHeaders headers = context.getMessage().getHeaders();
        Long actionBy = get(headers, MSG_KEY_ACTION_BY, Long.class, null);
        Integer orderNo = get(headers, MSG_KEY_ORDER_NO, Integer.class, null);
        String comment = get(headers, MSG_KEY_COMMENT, String.class, null);

        // check that the reviewer requesting changes is valid
        if (isUserIdInvalid(context.getStateMachine(), actionBy, "reject")) return false;

        // check that the user rejecting the application is in the admin list
        var adminList = (List<Long>) get(context, KEY_ADMIN_IDS, List.class, Collections.emptyList());
        if (adminList.contains(actionBy)) {
            return true;
        }

        // check that the reviewer belongs to the list of reviewers for the application.
        var reviewerMap = (Map<Integer, Set<Long>>) get(context, KEY_REVIEWERS_MAP, Map.class, Collections.emptyMap());
        Set<Long> reviewerList = reviewerMap.values().stream().flatMap(Set::stream).collect(Collectors.toSet());
        if (isUserAbsentFromUserList(context.getStateMachine(), reviewerList, actionBy, "reviewers",
                "reject")) return false;

        // check that the reviwer rejecting application has input a valid comment
        if (isCommentInvalid(context.getStateMachine(), comment, "reject")) return false;

        Pair<Integer, Long> lastForwardedBy = get(context, KEY_FORWARDED_BY_LAST, Pair.class, null);

        boolean isSerialFlow = get(context, KEY_APPROVAL_FLOW_TYPE, String.class, VAL_SERIAL).equalsIgnoreCase(VAL_SERIAL);
        if (isSerialFlow) { //TODO
            // if the application has been forwarded before:
            if (lastForwardedBy != null && lastForwardedBy.getSecond() != null) {
                // check that the current user is the last person to have forwarded the application,
                // i.e., no one has forwarded the application after the current user.
                Map<Integer, List<Pair<Long, Boolean>>> forwardMap = get(context, KEY_FORWARDED_MAP, Map.class,
                        Collections.emptyMap());

                boolean isNotLatestForwarder = forwardMap.entrySet()
                        .stream()
                        .filter(entry -> entry.getKey() > orderNo)
                        .anyMatch(entry -> entry.getValue().stream().anyMatch(Pair::getSecond));

                if (isNotLatestForwarder) {
                    String errorMsg =
                            "Guard failed for: " + "reject" + " as the reviewer: " + actionBy + " at position: " +
                                    orderNo + " cannot reject the application as there are other reviewers who have already forwarded the application after them";
                    context.getStateMachine().setStateMachineError(new StateMachineException(errorMsg));
                    return false;
                }
            } else { // if the application has not been forwarded before:
                // check that the current user is the first order user in the reviewerMap
                if (!reviewerMap.get(1).contains(actionBy)) {
                    String errorMsg =
                            "Guard failed for reject as the Reviewer: " + actionBy + " at position: " + orderNo + " " +
                                    "cannot reject the application as they are not present in the reviewer list";
                    context.getStateMachine().setStateMachineError(new StateMachineException(errorMsg));
                    return false;
                }
            }
        }

        return true;
    }

    public static boolean cancel(StateContext<String, String> context) {

        MessageHeaders headers = context.getMessage().getHeaders();
        Long actionBy = get(headers, MSG_KEY_ACTION_BY, Long.class, null);

        // check that the person requesting cancel is either an admin
        var adminIds = (List<Long>) get(context, KEY_ADMIN_IDS, List.class, Collections.emptyList());
        if (adminIds.contains(actionBy)) return true;

        // or the creator
        Long createdBy = get(context, KEY_CREATED_BY, Long.class, 0L);
        if (actionBy.equals(createdBy)) return true;

        return false;
    }

    private static boolean reviewCountCheck(StateContext<String, String> context) {
        int forwardedCount = get(context, KEY_FORWARDED_COUNT, Integer.class, 0);
        int reviewerCount = get(context, KEY_REVIEWERS_COUNT, Integer.class, 0);

        // check that the application has reviewers
        if (reviewerCount <= 0) {
            context.getStateMachine().setStateMachineError(new StateMachineException("Guard failed for forward " +
                    "as there are no reviewers for the application"));
            return false;
        }

        // check that the application is not forwarded more times than total reviewers.
        if (forwardedCount + 1 > reviewerCount) {
            context.getStateMachine().setStateMachineError(new StateMachineException("Guard failed for forward" +
                    " as the application is being forwarded more times than the number of defined reviewers"));
            return false;
        }

        return true;
    }

    /**
     * CHECKS
     **/


    private static boolean isUserAbsentFromUserList(StateMachine<String, String> statemachine, Collection<Long> userList,
            Long actionBy, String item, String transition) {
        if (!userList.isEmpty() && !userList.contains(actionBy)) {
            String errorMsg = "Guard failed on: " + transition + " as the user id: " + actionBy + " is not present in" +
                    " the " + item + " list: " + userList;
            statemachine.setStateMachineError(new StateMachineException(errorMsg));
            return true;
        }
        return false;
    }

    private static boolean isUserIdInvalid(StateMachine<String, String> statemachine, Long userId, String transition) {
        if (userId == null || userId == 0) {
            String errorMsg = "Guard failed on: " + transition + " as invalid userId: " + userId;
            statemachine.setStateMachineError(new StateMachineException(errorMsg));
            return true;
        }
        return false;
    }

    private static boolean isCommentInvalid(StateMachine<String, String> statemachine, String comment, String transition) {
        if (comment == null || comment.isBlank()) {
            String errorMsg = "Guard Failed for: " + transition + " as there is no valid comment provided";
            statemachine.setStateMachineError(new StateMachineException(errorMsg));
            return true;
        }
        return false;
    }

    private static boolean isCountExceedingThreshold(StateMachine<String, String> statemachine, Integer count, Integer threshold, String item, String transition) {
        if (count + 1 > threshold) {
            String errorMsg = "Guard Failed for: " + transition + " count: " + (count + 1) + " for item: " + item +
                    " " +
                    "exceeds threshold: " + threshold;
            statemachine.setStateMachineError(new StateMachineException(errorMsg));
            return true;
        }
        return false;
    }
}
