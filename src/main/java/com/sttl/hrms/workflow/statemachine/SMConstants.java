package com.sttl.hrms.workflow.statemachine;


public class SMConstants {


    private SMConstants() {
        // use class statically
    }

    public static final String LEAVE_APP_WF_V1 = "LeaveApplicationWorkflowStateMachineV1";
    public static final String LOAN_APP_WF_V1 = "LoanApplicationWorkflowStateMachineV1";

    // ExtendedState Keys
    public static final String KEY_ADMIN_IDS = "ADMIN_IDS";
    public static final String KEY_ANY_APPROVE = "ANY_APPROVE";
    public static final String KEY_APPROVAL_FLOW_TYPE = "APPROVAL_FLOW_TYPE";
    public static final String KEY_APPROVE_BY = "APPROVE_BY";
    public static final String KEY_APPROVE_COMMENT = "APPROVE_COMMENT";
    public static final String KEY_CLOSED_BY = "CLOSED_BY";
    public static final String KEY_CLOSED_COMMENT = "CLOSED_COMMENT";
    public static final String KEY_CLOSED_STATE_TYPE = "CLOSED_STATE_TYPE";
    public static final String KEY_FORWARDED_COMMENT = "FORWARDED_COMMENT";
    public static final String KEY_FORWARDED_COUNT = "FORWARDED_COUNT";
    public static final String KEY_FORWARDED_MAP = "FORWARDED_MAP";
    public static final String KEY_FORWARDED_BY_LAST = "LAST_FORWARDED_BY";
    public static final String KEY_CHANGE_REQ_MAX = "MAX_CHANGE_REQUESTS";
    public static final String KEY_CHANGE_REQ_COMMENT = "REQUESTED_CHANGE_COMMENT";
    public static final String KEY_CHANGES_REQ_BY = "REQUESTED_CHANGES_BY";
    public static final String KEY_REJECTED_BY = "REJECTED_BY";
    public static final String KEY_REJECTED_COMMENT = "REJECTED_COMMENT";
    public static final String KEY_RETURN_COUNT = "RETURN_COUNT";
    public static final String KEY_REVIEWERS_COUNT = "REVIEWERS_COUNT";
    public static final String KEY_REVIEWERS_MAP = "REVIEWERS_MAP";
    public static final String KEY_ROLL_BACK_BY_LAST = "LAST_ROLL_BACK_BY";
    public static final String KEY_ROLL_BACK_COMMENT = "ROLL_BACK_COMMENT";
    public static final String KEY_ROLL_BACK_COUNT = "ROLL_BACK_COUNT";
    public static final String KEY_ROLL_BACK_MAX = "ROLL_BACK_MAX";
    public static final String KEY_CREATED_BY = "APP_CREATED_BY";

    // ExtendedState Values
    public static final String VAL_APPROVED = "APPROVED";
    public static final String VAL_CANCELED = "CANCELED";
    public static final String VAL_PARALLEL = "PARALLEL";
    public static final String VAL_REJECTED = "REJECTED";
    public static final String VAL_SERIAL = "SERIAL";

    // Transactions
    public static final String TX_RVWR_APPROVES_APP_PARLL = "ReviewerApprovesTheApplicationUnderReview";
    public static final String TX_RVWR_APPROVES_APP_SERIAL = "ReviewerApprovesTheApplicationUnderReview";
    public static final String TX_RVWR_FWDS_APP = "ReviewerForwardsApplication";
    public static final String TX_RVWR_REJECTS_APP_PARLL = "ReviewerRejectsTheApplicationUnderReview";
    public static final String TX_RVWR_REJECTS_APP_SERIAL = "ReviewerRejectsTheApplicationUnderReview";
    public static final String TX_RVWR_REQ_CHANGES_FRM_USER_PARLL = "ReviewerRequestsChangesInTheApplicationUnderReview";
    public static final String TX_RVWR_REQ_CHANGES_FRM_USER_SERIAL = "ReviewerRequestsChangesInTheApplicationUnderReview";
    public static final String TX_RVWR_UNDO_APPRVL_PARLL = "ReviewerRollsBackApplicationUnderReviewParallel";
    public static final String TX_RVWR_UNDO_APPRVL_SERIAL = "ReviewerRollsBackTheApplicationUnderReview";
    public static final String TX_SYST_COMPLETES_APP = "SystemCompletesTheApplication";
    public static final String TX_SYST_TRGGRS_APPRVL_FLOW = "SystemTriggersTheApprovalFlowJunction";
    public static final String TX_SYST_TRGGRS_APP_FOR_REVIEW = "SystemTriggersTheSubmittedLeaveApplication";
    public static final String TX_USER_CANCELS_APP_PARLL = "UserCancelsTheApplicationUnderReviewInParallelFlow";
    public static final String TX_USER_CANCELS_APP_SERIAL = "UserCancelsTheApplicationUnderReview";
    public static final String TX_USER_CANCELS_APP_UNDER_REVIEW = "UserCancelsTheApplicationUnderReviewSerial";
    public static final String TX_USER_CANCELS_CREATED_APP = "UserCancelsTheSubmittedLeaveApplication";
    public static final String TX_USER_CREATES_APP = "UserCreatesTheApplication";
    public static final String TX_USER_SUBMITS_APP = "UserSubmitsTheCreatedApplication";

    // StateMachine Header Keys
    public static final String MSG_KEY_ORDER_NO = "order";
    public static final String MSG_KEY_ACTION_BY = "actionBy";
    public static final String MSG_KEY_COMMENT = "comment";


}
