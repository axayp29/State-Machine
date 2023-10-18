package com.sttl.hrms.workflow.statemachine.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessageHeaders;
import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateContext;

import java.util.Optional;

@Slf4j
public class ExtStateUtil {

    private ExtStateUtil(){}

    public static String getStateId(StateContext<String, String> context) {
        return context.getStateMachine().getState().getId();
    }

    public static <T> T get(StateContext<String, String> context, String key, Class<T> clazz, T defaultVal) {
        try {
            return get(context.getExtendedState(), key, clazz, defaultVal);
        } catch (Exception ex) {
            log.warn("Exception encountered getting value associated with key: {}", key, ex);
            return defaultVal;
        }
    }

    public static <T> T get(ExtendedState extendedState, String key, Class<T> clazz, T defaultVal) {
        try {
            return Optional.ofNullable(extendedState.get(key, clazz)).orElse(defaultVal);
        } catch (Exception ex) {
            log.warn("Exception encountered getting value associated with key: {}", key, ex);
            return defaultVal;
        }
    }

    public static <T> T get(MessageHeaders headers, String key, Class<T> clazz, T defaultVal) {
        try {
            return Optional.ofNullable(get(headers, key, clazz)).orElse(defaultVal);
        } catch (Exception ex) {
            log.warn("Exception encountered getting value associated with key: {}", key, ex);
            return defaultVal;
        }
    }

    public static <T> T get(MessageHeaders headers, String key, Class<T> clazz) {
        return headers.get(key, clazz);
    }
}
