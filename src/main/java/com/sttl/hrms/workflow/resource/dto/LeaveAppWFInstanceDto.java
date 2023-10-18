package com.sttl.hrms.workflow.resource.dto;

import com.sttl.hrms.workflow.data.Pair;
import com.sttl.hrms.workflow.data.enums.LeaveType;
import com.sttl.hrms.workflow.data.enums.WorkflowType;
import com.sttl.hrms.workflow.data.model.entity.LeaveAppWorkFlowInstanceEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.sttl.hrms.workflow.data.enums.WorkFlowTypeStateMachine.LEAVE_APP_SM;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class LeaveAppWFInstanceDto {

    // leave app
    @Builder.Default
    Short isActive = 1;
    @NotNull Integer leaveType;

    // workflow instance entity
    @NotNull WorkflowType typeId;
    @Builder.Default
    Short timesRolledBackCount = 0;
    @Builder.Default
    Short timesReturnedCount = 0;
    Short workflowVersion;
    @NotNull List<Pair<Integer, Set<Long>>> reviewers;

    // base entity
    @NotNull Long companyId;
    @NotNull Integer branchId;
    @Builder.Default
    LocalDateTime createDate = LocalDateTime.now();
    LocalDateTime updateDate;
    LocalDateTime deleteDate;
    Long createdByUserId;
    Long updatedByUserId;
    Long deletedByUserId;

    public static LeaveAppWorkFlowInstanceEntity toEntity(LeaveAppWFInstanceDto dto) {

        var entity = new LeaveAppWorkFlowInstanceEntity();

        // leave app workflow instance
        entity.setLeaveType(LeaveType.fromNumber(dto.getLeaveType()));
        entity.setIsActive(Optional.ofNullable(dto.getIsActive()).orElse((short) 1));

        List<Short> workflowVersions = LEAVE_APP_SM.getStateMachineIds()
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
        LEAVE_APP_SM.getStateMachineIds()
                .stream()
                .filter(v -> ("v" + entity.getWorkflowVersion()).equalsIgnoreCase(v.getFirst()))
                .map(Pair::getSecond)
                .findFirst()
                .ifPresent(entity::setStateMachineId);


        // workflow instance
        entity.setTypeId(dto.getTypeId());
        entity.setTimesRolledBackCount(dto.getTimesRolledBackCount());
        entity.setTimesReturnedCount(dto.getTimesReturnedCount());
        entity.setReviewers(dto.getReviewers());

        // base entity
        entity.setCompanyId(dto.getCompanyId());
        entity.setBranchId(dto.getBranchId());
        entity.setCreatedDate(dto.getCreateDate());
        entity.setUpdatedDate(dto.getUpdateDate());
        entity.setDeletedDate(dto.getDeleteDate());
        entity.setCreatedByUserId(dto.getCreatedByUserId());
        entity.setUpdatedByUserId(dto.getUpdatedByUserId());
        entity.setDeletedByUserId(dto.getDeletedByUserId());

        return entity;
    }
}
