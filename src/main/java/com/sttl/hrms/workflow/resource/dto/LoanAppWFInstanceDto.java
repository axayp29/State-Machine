package com.sttl.hrms.workflow.resource.dto;

import com.sttl.hrms.workflow.data.Pair;
import com.sttl.hrms.workflow.data.enums.LoanType;
import com.sttl.hrms.workflow.data.enums.WorkflowType;
import com.sttl.hrms.workflow.data.model.entity.LoanAppWorkflowInstanceEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.sttl.hrms.workflow.data.enums.WorkFlowTypeStateMachine.LOAN_APP_SM;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanAppWFInstanceDto {

    // loan app
    @Builder.Default
    Short isActive = 1;
    @NotNull Integer loanType;

    // workflow instance entity
    @NotNull WorkflowType typeId;
    @Builder.Default
    Short timesRolledBackCount = 0;
    @Builder.Default
    Short timesReturnedCount = 0;
    Short workflowVersion;
    @Builder.Default
    List<Pair<Integer, Set<Long>>> reviewers = new ArrayList<>();

    // base entity
    @NotNull Long companyId;
    @NotNull Integer branchId;
    LocalDateTime createDate;
    LocalDateTime updateDate;
    LocalDateTime deleteDate;
    Long createdByUserId;
    Long updatedByUserId;
    Long deletedByUserId;

    public static LoanAppWorkflowInstanceEntity toEntity(LoanAppWFInstanceDto dto) {
        var entity = new LoanAppWorkflowInstanceEntity();

        entity.setLoanType(LoanType.fromId(dto.getLoanType()));
        entity.setIsActive(Optional.ofNullable(dto.getIsActive()).orElse((short) 1));

        List<Short> workflowVersions = LOAN_APP_SM.getStateMachineIds()
                .stream()
                .map(Pair::getFirst)
                .map(ver -> Short.parseShort(ver.substring(1)))
                .toList();

        // set the specified workflow version, if not present, set latest version.
        Optional.ofNullable(dto.getWorkflowVersion())
                .filter(workflowVersions::contains)
                .or(() -> workflowVersions.stream().max(Short::compare))
                .ifPresent(entity::setWorkflowVersion);

        // set the stateMachineId according to the workflow version
        LOAN_APP_SM.getStateMachineIds()
                .stream()
                .filter(v -> ("v" + entity.getWorkflowVersion()).equalsIgnoreCase(v.getFirst()))
                .map(Pair::getSecond)
                .findFirst()
                .ifPresent(entity::setStateMachineId);

        // workflowinstance dto
        entity.setTypeId(dto.getTypeId());
        entity.setTimesRolledBackCount(dto.getTimesRolledBackCount());
        entity.setTimesReturnedCount(dto.getTimesReturnedCount());
        entity.setReviewers(dto.getReviewers());

        // base dto
        entity.setCompanyId(dto.getCompanyId());
        entity.setBranchId(dto.getBranchId());
        entity.setCreatedByUserId(dto.getCreatedByUserId());
        entity.setUpdatedByUserId(dto.getUpdatedByUserId());
        entity.setDeletedByUserId(dto.getDeletedByUserId());
        entity.setCreatedDate(dto.getCreateDate());
        entity.setUpdatedDate(dto.getUpdateDate());
        entity.setDeletedDate(dto.getDeleteDate());

        return entity;

    }
}
