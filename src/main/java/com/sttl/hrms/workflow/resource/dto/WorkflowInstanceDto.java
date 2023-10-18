package com.sttl.hrms.workflow.resource.dto;

import com.sttl.hrms.workflow.data.Pair;
import com.sttl.hrms.workflow.data.enums.WorkflowType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowInstanceDto {

    // base entity
    @NotNull
    Long companyId;
    @NotNull
    Integer branchId;
    LocalDateTime createDate;
    LocalDateTime updateDate;
    LocalDateTime deleteDate;
    Long createdByUserId;
    Long updatedByUserId;
    Long deletedByUserId;

    // workflow instance entity
    @NotNull
    WorkflowType typeId;
    @Builder.Default
    Short timesRolledBackCount = 0;
    @Builder.Default
    Short timesReturnedCount = 0;
    Short workflowVersion;
    List<Pair<Integer, Long>> reviewers;
}
