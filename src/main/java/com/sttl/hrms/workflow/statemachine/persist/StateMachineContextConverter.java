package com.sttl.hrms.workflow.statemachine.persist;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.sttl.hrms.workflow.statemachine.exception.StateMachinePersistenceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessageHeaders;
import org.springframework.statemachine.kryo.MessageHeadersSerializer;
import org.springframework.statemachine.kryo.StateMachineContextSerializer;
import org.springframework.statemachine.kryo.UUIDSerializer;
import org.springframework.statemachine.support.DefaultStateMachineContext;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

@SuppressWarnings("rawtypes") // StateMachineContext is used without generics.
@RequiredArgsConstructor
@Converter(autoApply = true)
@Slf4j
public class StateMachineContextConverter implements AttributeConverter<DefaultStateMachineContext, byte[]>, AutoCloseable {

    private static final ThreadLocal<Kryo> kryoThreadLocal = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();
        kryo.addDefaultSerializer(DefaultStateMachineContext.class, new StateMachineContextSerializer());
        kryo.addDefaultSerializer(MessageHeaders.class, new MessageHeadersSerializer());
        kryo.addDefaultSerializer(UUID.class, new UUIDSerializer());
        return kryo;
    });

    @Override
    public void close() {
        kryoThreadLocal.remove();
    }

    @Override
    public byte[] convertToDatabaseColumn(DefaultStateMachineContext attribute) {
        return serialize(attribute);
    }

    @Override
    public DefaultStateMachineContext convertToEntityAttribute(byte[] dbData) {
        return deserialize(dbData);
    }

    private byte[] serialize(DefaultStateMachineContext context) {
        if (context == null) {
            throw new StateMachinePersistenceException("Exception encountered when serializing state machine context to byte array",
                    new NullPointerException("StateMachineContext is null"));
        }

        Kryo kryo = kryoThreadLocal.get();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            try (Output output = new Output(out)) {
                kryo.writeObject(output, context);
            }
            byte[] outByteArr = out.toByteArray();
            log.debug("StateMachineContext: {}, serialized and written to thread local as byte array of: {} bytes",
                    context.getId(), outByteArr.length);
            return outByteArr;
        } catch (IOException e) {
            throw new StateMachinePersistenceException("Exception encountered when serializing state machine context to byte array", e);
        }
    }

    private DefaultStateMachineContext deserialize(byte[] data) {
        if (data == null || data.length == 0) {
            throw new StateMachinePersistenceException("Exception encountered when deserializing byte array to state machine context",
                    new NullPointerException("byteArray is null"));
        }

        Kryo kryo = kryoThreadLocal.get();

        try (ByteArrayInputStream in = new ByteArrayInputStream(data)) {
            try (Input input = new Input(in)) {
                DefaultStateMachineContext stateMachineContext = kryo.readObject(input, DefaultStateMachineContext.class);
                log.debug("StateMachineContext: {}, deserialized from thread local byte array of: {} bytes",
                        stateMachineContext.getId(), data.length);
                return stateMachineContext;
            }
        } catch (IOException e) {
            throw new StateMachinePersistenceException("Exception encountered when deserializing byte array to state machine context", e);
        }
    }
}
