package com.sttl.hrms.workflow.resource.dto;

import com.sttl.hrms.workflow.data.enums.WorkflowType;
import com.sttl.hrms.workflow.data.model.entity.WorkflowTypeEntity;
import com.sttl.hrms.workflow.data.model.entity.WorkflowTypeEntity.WorkflowProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class WorkflowTypeDto {

    // base entity
    @NotNull Long companyId;
    @NotNull Integer branchId;
    LocalDateTime createDate;
    LocalDateTime updateDate;
    LocalDateTime deleteDate;
    Long createdByUserId;
    Long updatedByUserId;
    Long deletedByUserId;

    // workflow type
    LocalDateTime withEffectFromDate;
    Integer workflowTypeId;
    Short isActive;

    // workflow properties
    WorkflowPropertiesDto wfPropDto = new WorkflowPropertiesDto();

    public static WorkflowTypeEntity toEntity(WorkflowTypeDto dto) {
        WorkflowTypeEntity entity = new WorkflowTypeEntity();

        entity.setCompanyId(dto.getCompanyId());
        entity.setBranchId(dto.getBranchId());
        if (dto.getCreateDate() != null) entity.setCreatedDate(dto.getCreateDate());
        if (dto.getUpdateDate() != null) entity.setUpdatedDate(dto.getUpdateDate());
        if (dto.getDeleteDate() != null) entity.setDeletedDate(dto.getDeleteDate());

        // to create a new workflow type, its entry should be present in the WorkflowType enum.
        WorkflowType type = WorkflowType.fromId(dto.workflowTypeId);
        entity.setTypeId(type);
        entity.setName(type);
        entity.setWithEffectFromDate(dto.withEffectFromDate);
        if (dto.isActive != null) entity.setIsActive(dto.isActive);
        if (dto.updatedByUserId != null) entity.setUpdatedByUserId(dto.updatedByUserId);
        if (dto.createdByUserId != null) entity.setCreatedByUserId(dto.createdByUserId);
        if (dto.deletedByUserId != null) entity.setDeletedByUserId(dto.deletedByUserId);

        WorkflowProperties properties = WorkflowPropertiesDto.toProp(dto.wfPropDto);
        entity.setWorkflowProperties(properties);

        return entity;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class WorkflowPropertiesDto {

        // workflow properties
        Boolean parallelApproval;
        Boolean repeatableApprovers;
        Boolean rollBackApproval;
        Boolean adminApproveWorkflow;
        List<Long> adminRoleIds = new ArrayList<>();
        Integer changeReqMaxCount;
        Integer rollbackMaxCount;
        Boolean singleApproval;

        public static WorkflowProperties toProp(WorkflowPropertiesDto wfPropDto) {
            WorkflowProperties properties = new WorkflowProperties();

            Optional.of(wfPropDto)
                    .flatMap(dto -> Optional.ofNullable(dto.getParallelApproval()))
                    .ifPresent(properties::setParallelApproval);

            Optional.of(wfPropDto)
                    .flatMap(dto -> Optional.ofNullable(dto.getRepeatableApprovers()))
                    .ifPresent(properties::setRepeatableApprovers);

            Optional.of(wfPropDto)
                    .flatMap(dto -> Optional.ofNullable(dto.getRollBackApproval()))
                    .ifPresent(properties::setRollBackApproval);

            Optional.of(wfPropDto)
                    .flatMap(dto -> Optional.ofNullable(dto.getAdminApproveWorkflow()))
                    .ifPresent(properties::setAdminApproveWorkflow);

            Optional.of(wfPropDto)
                    .flatMap(dto -> Optional.ofNullable(dto.getAdminRoleIds()))
                    .ifPresent(properties::setAdminRoleIds);

            Optional.of(wfPropDto)
                    .flatMap(dto -> Optional.ofNullable(dto.getChangeReqMaxCount()))
                    .ifPresent(properties::setChangeReqMaxCount);

            Optional.of(wfPropDto)
                    .flatMap(dto -> Optional.ofNullable(dto.getRollbackMaxCount()))
                    .ifPresent(properties::setRollbackMaxCount);

            Optional.of(wfPropDto)
                    .flatMap(dto -> Optional.ofNullable(dto.getSingleApproval()))
                    .ifPresent(properties::setSingleApproval);

            return properties;
        }
    }
}
