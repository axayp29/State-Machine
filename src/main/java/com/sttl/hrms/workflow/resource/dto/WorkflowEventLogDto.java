package com.sttl.hrms.workflow.resource.dto;

import com.sttl.hrms.workflow.data.enums.WorkflowType;
import com.sttl.hrms.workflow.data.model.entity.WorkflowEventLogEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@AllArgsConstructor
@Builder
@Getter
@NoArgsConstructor
public class WorkflowEventLogDto {

    private Long id;

    @NotNull
    private Long companyId;

    @NotNull
    private Integer branchId;

    private Integer typeId;

    private Long instanceId;

    private String state;

    private String event;

    private LocalDateTime actionDate;

    private Long actionBy;

    private Short userRole;

    private Short completed;

    private String comment;
    
    private String exception;

    public static WorkflowEventLogEntity toEntity(WorkflowEventLogDto dto) {
        return WorkflowEventLogEntity.builder()
                .id(dto.getId())
                .companyId(dto.getCompanyId())
                .branchId(dto.getBranchId())
                .typeId(WorkflowType.fromId(dto.getTypeId()))
                .instanceId(dto.getInstanceId())
                .state(dto.getState())
                .event(dto.getEvent())
                .actionDate(dto.getActionDate())
                .actionBy(dto.getActionBy())
                .userRole(dto.getUserRole())
                .completed(dto.getCompleted())
                .comment(dto.getComment())
                .build();
    }

}
