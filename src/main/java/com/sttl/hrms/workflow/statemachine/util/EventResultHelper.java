package com.sttl.hrms.workflow.statemachine.util;

import com.sttl.hrms.workflow.statemachine.EventResultDto;
import com.sttl.hrms.workflow.statemachine.exception.StateMachineException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.StateMachineEventResult;
import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
public class EventResultHelper {

    private EventResultHelper() {
        // use class statically
    }

    public static List<EventResultDto> toResultDTOList(Flux<StateMachineEventResult<String, String>> resultFlux) {
        List<EventResultDto> eventResults = resultFlux
                .toStream()
                .map(EventResultDto::new)
                .peek(result -> log.trace("event result: {}", result))
                .toList();
        log.trace("Parsing StateMachine event results to list: {}", eventResults);
        return eventResults;
    }

    public static String toResultDTOString(Flux<StateMachineEventResult<String, String>> resultFlux) {
        return "[" + Optional
                .ofNullable(toResultDTOList(resultFlux))
                .or(() -> Optional.of(Collections.emptyList()))
                .stream()
                .flatMap(Collection::stream)
                .map(EventResultDto::toString)
                .collect(Collectors.joining(",\n")) + "]";
    }

    public static List<EventResultDto> processResultFlux(Flux<StateMachineEventResult<String, String>> resultFlux) {
        // parse the result
        List<EventResultDto> resultDTOList = EventResultHelper.toResultDTOList(resultFlux);
        log.debug("resultFlux is: {}", resultDTOList);

        // empty result
        if (resultDTOList == null || resultDTOList.isEmpty()) {
            log.warn("state machine event result was empty");
            return Collections.emptyList();
        }

        // find out if any event wasn't accepted.
        boolean hasErrors = resultDTOList.stream().anyMatch(Predicate.not(EventResultDto.accepted));

        // throw exception if the event is not accepted by the state machine.
        if (hasErrors) {
            String eventStr = resultDTOList
                    .stream()
                    .filter(Predicate.not(EventResultDto.accepted))
                    .map(EventResultDto::getEvent)
                    .map(StringUtil::event)
                    .collect(Collectors.joining(", "));
            String msg = "The following passed events: [" + eventStr + "] were not accepted by the statemachine ";
            throw new StateMachineException(msg);
        }

        return resultDTOList;
    }

}
