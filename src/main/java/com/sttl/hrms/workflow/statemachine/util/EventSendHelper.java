package com.sttl.hrms.workflow.statemachine.util;

import com.sttl.hrms.workflow.data.Pair;
import com.sttl.hrms.workflow.resource.dto.PassEventDto;
import com.sttl.hrms.workflow.statemachine.EventResultDto;
import com.sttl.hrms.workflow.statemachine.exception.StateMachineException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Predicate;

import static com.sttl.hrms.workflow.statemachine.SMConstants.*;

@Slf4j
public class EventSendHelper {

    private EventSendHelper() {
        // use class statically
    }

    public static Pair<StateMachine<String, String>, List<EventResultDto>> passEvents(StateMachine<String,
            String> stateMachine, List<PassEventDto> eventDtos) {

        StateMachine<String, String> resultStateMachine = stateMachine;
        List<EventResultDto> results = new ArrayList<>();

        for (PassEventDto eventDto : eventDtos) {
            if (!resultStateMachine.hasStateMachineError()) {
                var eventResult = passEvent(resultStateMachine, eventDto);

                resultStateMachine = eventResult.getFirst();
                if (!eventResult.getSecond().isEmpty()) results.addAll(eventResult.getSecond());
            } else log.error("Could not pass event: {} to statemachine: {} as it has an error", eventDto.getEvent(),
                    stateMachine.getId());
        }

        return new Pair<>(resultStateMachine, results);
    }

    public static Pair<StateMachine<String, String>, List<EventResultDto>> passEvent(StateMachine<String, String> stateMachine,
            PassEventDto eventDto) {

        Map<String, Object> headersMap = new HashMap<>();
        Optional.ofNullable(eventDto.getOrderNo()).ifPresent(ord -> headersMap.put(MSG_KEY_ORDER_NO, ord));
        Optional.ofNullable(eventDto.getActionBy()).ifPresent(actBy -> headersMap.put(MSG_KEY_ACTION_BY, actBy));
        Optional.ofNullable(eventDto.getComment()).filter(Predicate.not(String::isBlank))
                .ifPresent(cmt -> headersMap.put(MSG_KEY_COMMENT, cmt));
        log.debug("Passing Message to statemachine: {} with event: {} and headers: {}", stateMachine.getId(),
                eventDto.getEvent(), headersMap);

        // parse the result
        List<EventResultDto> resultDTOList = sendMessageToSM(stateMachine, eventDto.getEvent(), headersMap);
        log.debug("After passing event: {}, resultFlux is: {}", eventDto.getEvent(), resultDTOList);
        return new Pair<>(stateMachine, resultDTOList);
    }

    public static List<EventResultDto> sendMessageToSM(StateMachine<String, String> stateMachine,
            String event, Map<String, Object> headersMap) {
        MessageBuilder<String> msgBldr = MessageBuilder.withPayload(event);
        msgBldr.copyHeaders(headersMap);
        Message<String> message = msgBldr.build();
        log.debug("Passing Message to statemachine: {} with event: {} and headers: {}", stateMachine.getId(), message.getPayload(),
                message.getHeaders());

        try {
            return EventResultHelper.processResultFlux(stateMachine.sendEvent(Mono.just(message)));
        } catch (Exception ex) {
            throw new StateMachineException("Could not send event: " + event + " to the statemachine: " + stateMachine.getId(), ex);
        }
    }

    public static List<EventResultDto> sendMessagesToSM(StateMachine<String, String> stateMachine,
            Map<String, Object> headersMap, String... events) {
        List<EventResultDto> results = new ArrayList<>();
        for (String event : events) {
            results.addAll(sendMessageToSM(stateMachine, event, headersMap));
        }
        return results;
    }
}
