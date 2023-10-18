package com.sttl.hrms.workflow.resource.dto;

import com.sttl.hrms.workflow.data.enums.WorkflowType;
import com.sttl.hrms.workflow.statemachine.EventResultDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventResponseDto {
    private Long workflowInstance;
    private String workflowType;
    private String currentState;
    private String event;
    private String resultType;
    private boolean isComplete;

    public static EventResponseDto fromEventResult(Long id, WorkflowType type, EventResultDto eventResultDTO) {
        return EventResponseDto.builder()
                .workflowInstance(id)
                .workflowType(type.getName())
                .currentState(eventResultDTO.getCurrentState())
                .event(eventResultDTO.getEvent())
                .resultType(eventResultDTO.getResultType().name())
                .isComplete(eventResultDTO.isComplete())
                .build();
    }

    public static List<EventResponseDto> fromEventResults(Long id, WorkflowType type, List<EventResultDto> eventResultDtoList) {
        return eventResultDtoList
                .stream()
                .map(result -> EventResponseDto.fromEventResult(id, type, result))
                .toList();
    }
}
