package com.sttl.hrms.workflow.resource.dto;

import com.sttl.hrms.workflow.data.enums.WorkflowType;
import com.sttl.hrms.workflow.statemachine.builder.StateMachineBuilder.SMEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class PassEventDto {
    @NotNull
    WorkflowType workflowType;
    @NotNull
    Long workflowInstanceId;
    @NotNull
    String event;
    @NotNull
    Long actionBy;
    @Nullable
    @Builder.Default
    LocalDateTime actionDate = LocalDateTime.now();
    @Nullable
    Integer orderNo;
    @Nullable
    String comment;

    public static List<PassEventDto> createPassEvents(PassEventDto passEvent, SMEvent... events) {
        List<PassEventDto> passEvents = new ArrayList<>(events.length + 1);
        passEvents.add(passEvent);
        for (SMEvent event : events) {
            passEvents.add(PassEventDto.builder()
                    .workflowType(passEvent.getWorkflowType())
                    .workflowInstanceId(passEvent.getWorkflowInstanceId())
                    .event(event.name())
                    .actionBy(passEvent.getActionBy())
                    .actionDate(passEvent.getActionDate())
                    .build());
        }
        return passEvents;
    }
}
