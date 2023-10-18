package com.sttl.hrms.workflow.data.enums;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum LoanType {
    MULTIPURPOSE_ADVANCE(1),
    COMPUTER_LOAN(2),
    SCOOTER_ADVANCE(3),
    CAR_ADVANCE(4),
    HOUSE_BUILDING_ADVANCE_NEW_CONSTRUCTION(5),
    HOUSE_BUILDING_ADVANCE_RENOVATION(6),
    FESTIVAL_ADVANCE(7);

    private final int id;
    private static final LoanType[] values = LoanType.values();

    public static LoanType fromId(int id) {
        for (LoanType loanType : values) {
            if (loanType.id == id)
                return loanType;
        }
        throw new IllegalArgumentException("No loan type found for given id");
    }
}
