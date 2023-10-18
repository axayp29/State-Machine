package com.sttl.hrms.workflow.statemachine;

import lombok.*;
import org.springframework.messaging.MessageHeaders;
import org.springframework.statemachine.StateMachineEventResult;
import org.springframework.statemachine.StateMachineEventResult.ResultType;
import org.springframework.statemachine.region.Region;
import org.springframework.statemachine.state.State;

import java.util.Optional;
import java.util.function.Predicate;

import static com.sttl.hrms.workflow.statemachine.SMConstants.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventResultDto {

    private String region;
    private boolean isSubMachine;
    private String event;
    private ResultType resultType;
    private String currentState;
    private boolean isComplete;
    private Integer order;
    private Long actionBy;
    private String comment;

    public static final Predicate<EventResultDto> accepted = result -> result.getResultType()
            .equals(ResultType.ACCEPTED) || result.getResultType().equals(ResultType.DEFERRED);

    public EventResultDto(StateMachineEventResult<String, String> result) {
        Region<String, String> smRegion = result.getRegion();
        this.setRegion(smRegion.getUuid().toString() + " (" + smRegion.getId() + ")");
        this.isSubMachine = Optional.ofNullable(smRegion.getState()).map(State::isSubmachineState).orElse(false);
        this.setEvent(result.getMessage().getPayload());
        this.setResultType(result.getResultType());
        Optional.ofNullable(smRegion.getState()).map(State::getId).ifPresent(this::setCurrentState);
        this.setComplete(smRegion.isComplete());

        MessageHeaders headers = result.getMessage().getHeaders();
        Optional.ofNullable(headers.get(MSG_KEY_ORDER_NO, Integer.class)).ifPresent(this::setOrder);
        Optional.ofNullable(headers.get(MSG_KEY_ACTION_BY, Long.class)).ifPresent(this::setActionBy);
        Optional.ofNullable(headers.get(MSG_KEY_COMMENT, String.class)).filter(Predicate.not(String::isBlank))
                .ifPresent(this::setComment);
    }

    @Override
    public String toString() {
        String submachine = this.isSubMachine ? ", isSubMachine: true" : "";
        String complete = this.isComplete ? ", isComplete: true" : "";
        String order = (this.order != null) ? ", order: " + this.order : "";
        String actionBy = (this.actionBy != null) ? ", actionBy: " + this.actionBy : "";
        String comment = (this.comment != null) ? ", comment: " + this.comment : "";
        return "EventResultDto[" +
                "stateMachine: " + this.region +
                ", currentState: " + this.currentState +
                ", event: " + this.event +
                ", resultType: " + this.resultType +
                order +
                actionBy +
                comment +
                submachine +
                complete +
                "]";
    }
}
