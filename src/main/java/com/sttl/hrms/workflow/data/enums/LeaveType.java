package com.sttl.hrms.workflow.data.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LeaveType {
    EL(1, "Earned Leave"),
    LND(2, "Leave Not Due"),
    ML(3, "Maternity Leave"),
    SCL(4, "Special Casual Leave"),
    CL(5, "Casual Leave"),
    LHP(6, "Leave on Half Pay"),
    CML(7, "Commuted Leave"),
    EOL(8, "Extra Ordinary Leave"),
    SDL(9, "Special Disability Leave"),
    RH(10, "Restricted Holiday"),
    SEL(11, "Special Election Leave"),
    CCL(12, "Child Care Leave"),
    MML(13, "Miscarriage Leave"),
    PL(14, "Paternity Leave"),
    CAL(15, "Child Adoption Leave");

    private final int number;
    private final String leaveName;
    private static final LeaveType[] values = LeaveType.values();

    /**
     * Method to return a LeaveApp enum that matches the leave type id. Can be used to map JSON m
     *
     * @param number the id to be matched against the leave type enum number
     * @return LeaveType leaveType that matches the given number
     */
    public static LeaveType fromNumber(int number) {
        for (LeaveType lt : values) {
            if (lt.getNumber() == number)
                return lt;
        }
        throw new IllegalArgumentException("No leave type found for the specified number");
    }
}
