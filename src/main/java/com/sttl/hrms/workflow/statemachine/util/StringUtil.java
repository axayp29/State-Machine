package com.sttl.hrms.workflow.statemachine.util;

import com.sttl.hrms.workflow.statemachine.EventResultDto;
import lombok.experimental.UtilityClass;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineEventResult;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.transition.Transition;
import reactor.core.publisher.Flux;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

@UtilityClass
public class StringUtil {

    public static String state(State<String, String> state) {
        return Optional.ofNullable(state)
                .map(State::getId)
                .orElse("null");
    }

    public static String stageFromContext(StateContext<String, String> stateContext) {
        return Optional
                .ofNullable(stateContext)
                .flatMap(sc -> Optional.ofNullable(sc.getStage()))
                .flatMap(stage -> Optional.of(stage.name()))
                .orElse("null");
    }

    public static String sourceStateFromContext(StateContext<String, String> stateContext) {
        return Optional
                .ofNullable(stateContext)
                .flatMap(sc -> Optional.ofNullable(sc.getSource()))
                .map(State::getId)
                .orElse("null");
    }

    public static String eventFromContext(StateContext<String, String> stateContext) {
        return Optional
                .ofNullable(stateContext)
                .map(StateContext::getEvent)
                .map(StringUtil::event)
                .orElse("null");
    }

    public static String event(String event) {
        return String.valueOf(event);
    }

    public static String targetStateFromContext(StateContext<String, String> stateContext) {
        return Optional
                .ofNullable(stateContext)
                .flatMap(sc -> Optional.ofNullable(sc.getTarget()))
                .map(State::getId)
                .orElse("null");
    }

    public static String sourceStateFromTransition(Transition<String, String> transition) {
        return Optional
                .ofNullable(transition)
                .map(Transition::getSource)
                .map(State::getId)
                .orElse("null");
    }

    public static String targetStateFromTransition(Transition<String, String> transition) {
        return Optional
                .ofNullable(transition)
                .map(Transition::getTarget)
                .map(State::getId)
                .orElse("null");
    }

    public static String extendedStateFromContext(StateContext<String, String> stateContext) {
        return Optional
                .ofNullable(stateContext)
                .flatMap(sc -> Optional.ofNullable(sc.getExtendedState()))
                .flatMap(es -> Optional.ofNullable(es.getVariables()))
                .filter(Predicate.not(Map::isEmpty))
                .map(Map::entrySet)
                .map(StringUtil::entrySet)
                .filter(Predicate.not(String::isEmpty))
                .orElse("null");
    }

    public static String extendedState(ExtendedState extendedState) {
        return Optional.ofNullable(extendedState)
                .map(ExtendedState::getVariables)
                .filter(Predicate.not(Map::isEmpty))
                .map(Map::entrySet)
                .map(StringUtil::entrySet)
                .orElse("");
    }

    public static String messageHeaders(MessageHeaders headers) {
        return Optional.ofNullable(headers)
                .filter(Predicate.not(MessageHeaders::isEmpty))
                .map(MessageHeaders::entrySet)
                .map(StringUtil::entrySet)
                .orElse("");
    }

    private static String object(Object object) {
        return Optional
                .ofNullable(object)
                .map(Object::toString)
                .orElse("");
    }

    private static <K, V> String entrySet(Set<Entry<K, V>> entrySet) {
        if (entrySet == null)
            return "";
        if (entrySet.isEmpty())
            return "";
        return entrySet
                .stream()
                .map(entry -> "{key: " + object(entry.getKey()) + ", value: " + object(entry.getValue()) + "}")
                .collect(joining(", "));
    }

    public static String transition(Transition<String, String> transition) {
        if (transition == null)
            return "";

        String name = Optional.ofNullable(transition.getName())
                .map(Object::toString)
                .filter(Predicate.not(String::isEmpty))
                .map(s -> "name: " + s)
                .orElse("");

        String sourceState = Optional.ofNullable(transition.getSource())
                .map(State::getId)
                .map(Object::toString)
                .filter(Predicate.not(String::isEmpty))
                .map(s -> "{source state: " + s + (transition.getSource()
                        .isSubmachineState() ? ", isSubMachine: true" : "") + "}")
                .orElse("");

        String targetState = Optional.ofNullable(transition.getTarget())
                .map(State::getId)
                .map(Object::toString)
                .filter(Predicate.not(String::isEmpty))
                .map(s -> "{target state: " + s + (transition.getTarget()
                        .isSubmachineState() ? ", isSubMachine: true" : "") + "}")
                .orElse("");

        String event = Optional.ofNullable(transition.getTrigger())
                .flatMap(trigger -> Optional.ofNullable(trigger.getEvent()))
                .map(e -> "event: " + e)
                .orElse("");

        String kind = Optional.ofNullable(transition.getKind())
                .map(Object::toString)
                .filter(Predicate.not(String::isEmpty))
                .map(s -> "kind: " + s)
                .orElse("");

        return Stream.of(name, sourceState, event, targetState, kind) //, guard, actions, trigger)
                .filter(Predicate.not(String::isEmpty).and(Predicate.not(String::isBlank)))
                .collect(joining(", "));

    }

    public static String stateMachine(StateMachine<String, String> stateMachine, boolean detailed) {
        if (stateMachine == null) {
            return "";
        }

        String stateStr = state(stateMachine.getState());
        String idStr = stateMachine.getId();
        String output = "StateMachine[id: " + idStr + ", currentState: " + stateStr;

        if (detailed) {
            String extendedStateStr = extendedState(stateMachine.getExtendedState());
            String uuidStr = stateMachine.getUuid().toString();
            String allStatesStr = stateMachine.getStates().stream().map(StringUtil::state).collect(joining(", "));
            String initialStateStr = StringUtil.state(stateMachine.getInitialState());
            String transitionsStr = stateMachine.getTransitions().stream().map(StringUtil::transition)
                    .collect(joining(",\n"));
            output += ",\n uuid: " + uuidStr +
                    ",\n extendedState: " + extendedStateStr +
                    ",\n initialState: " + initialStateStr +
                    ",\n allStates: {" + allStatesStr + "}" +
                    ",\n transitions: {" + transitionsStr + "}";
        }

        return output + "]";
    }

    public static String messageHeaders(Message<String> message) {
        String headers = Optional.ofNullable(message)
                .map(Message::getHeaders)
                .stream()
                .flatMap(h -> h.entrySet().stream())
                .filter(e -> !e.getKey().equalsIgnoreCase("id"))
                .filter(e -> !e.getKey().equalsIgnoreCase("timestamp"))
                .map(e -> "{key: " + e.getKey() + ", value: " + e.getValue() + "}")
                .collect(Collectors.joining(", "));
        return headers.isEmpty() ? "" : "headers: [" + headers + "]";
    }

    public static String stateMachineEventResult(Flux<StateMachineEventResult<String, String>> resultsFlux) {
        return "[" + resultsFlux.toStream()
                .map(EventResultDto::new)
                .map(EventResultDto::toString)
                .collect(Collectors.joining(",\n")) + "]";
    }

    public static <T> String classDelta(T obj1, T ob2) {
        Field[] obj1Fields = obj1.getClass().getDeclaredFields();
        Field[] obj2Fields = ob2.getClass().getDeclaredFields();
        StringBuilder changes = new StringBuilder(obj1.getClass().getSimpleName()).append("{");
        for (Field obj1Field : obj1Fields) {
            for (Field obj2Field : obj2Fields) {
                try {
                    obj1Field.setAccessible(true);
                    obj2Field.setAccessible(true);
                    final String obj1FieldName = obj1Field.getName();
                    final String obj2FieldName = obj2Field.getName();
                    final Object obj1Value = obj1Field.get(obj1);
                    final Object obj2Value = obj2Field.get(ob2);
                    if (obj1FieldName.equalsIgnoreCase(obj2FieldName) && obj1Value != null && !obj1Value.equals(obj2Value)) {
                        changes.append(" ").append(obj1FieldName).append(": ").append(obj1Value).append(" | ")
                                .append(obj2Value).append(",");
                    }
                } catch (IllegalAccessException ex) {
                    changes.append("error: ").append(ex.getMessage());
                }
            }
        }
        if (changes.length() > 1) changes.deleteCharAt(changes.length() - 1);
        changes.append("}");
        return changes.toString();
    }

    public static <T> String beanDelta(T obj1, T obj2) {
        StringBuilder changes = new StringBuilder();
        try {
            BeanInfo obj1Info = Introspector.getBeanInfo(obj1.getClass());
            BeanInfo obj2Info = Introspector.getBeanInfo(obj2.getClass());
            changes.append(obj1Info.getClass().getSimpleName()).append("{");
            for (PropertyDescriptor obj1Prop : obj1Info.getPropertyDescriptors()) {
                for (PropertyDescriptor obj2Prop : obj2Info.getPropertyDescriptors()) {
                    if (obj1Prop != null && obj2Prop != null && obj1Prop.getName().equalsIgnoreCase(obj2Prop.getName())) {
                        String obj1PropVal = Optional.of(obj1Prop)
                                .flatMap(prop -> Optional.ofNullable(prop.getReadMethod()))
                                .flatMap(method -> Optional.ofNullable(methodInvoke(method, obj1)))
                                .map(Object::toString)
                                .map(val -> val.length() > 100 ? val.substring(0, 100).concat("...") : val)
                                .orElse("<null>");

                        String obj2PropVal = Optional.of(obj2Prop)
                                .flatMap(prop -> Optional.ofNullable(prop.getReadMethod()))
                                .flatMap(method -> Optional.ofNullable(methodInvoke(method, obj2)))
                                .map(Object::toString)
                                .map(val -> val.length() > 100 ? val.substring(0, 100).concat("...") : val)
                                .orElse("<null>");
                        if (!obj1PropVal.equalsIgnoreCase(obj2PropVal)) {
                            changes.append(" ").append(obj1Prop.getName()).append(": ")
                                    .append(obj1PropVal).append(" | ").append(obj2PropVal).append(",");
                        }
                    }
                }
            }
        } catch (IntrospectionException ex) {
            changes.append("error: ").append(ex.getMessage());
        }
        if (changes.length() > 1) changes.deleteCharAt(changes.length() - 1);
        changes.append("}");
        return changes.toString();
    }

    private static <T> Object methodInvoke(Method method, T obj) {
        try {
            return method.invoke(obj);
        } catch (InvocationTargetException | IllegalAccessException ex) {
            return null;
        }
    }

}
