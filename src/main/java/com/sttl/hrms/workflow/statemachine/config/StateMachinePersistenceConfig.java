package com.sttl.hrms.workflow.statemachine.config;

import com.sttl.hrms.workflow.data.model.entity.WorkflowInstanceEntity;
import com.sttl.hrms.workflow.statemachine.persist.DefaultStateMachineAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.StateMachinePersist;
import org.springframework.statemachine.persist.DefaultStateMachinePersister;
import org.springframework.statemachine.persist.StateMachinePersister;
import org.springframework.statemachine.support.DefaultStateMachineContext;

@Configuration
public class StateMachinePersistenceConfig {

    /**
     * You cannot persist a StateMachine by using normal java serialization, as the object graph is too rich and contains too many dependencies on other Spring
     * context classes. StateMachineContext is a runtime representation of a state machine, that you can use to restore an existing machine into a state
     * represented by a particular StateMachineContext object.
     *
     * @return An instance of the StateMachinePersist interface, responsible for serialization and deserialization of a StateMachineContext
     */
    @Bean
    public StateMachinePersist<String, String, WorkflowInstanceEntity> stateMachinePersist() {
        return new StateMachinePersist<>() {
            @SuppressWarnings("RedundantThrows")
            @Override
            public void write(StateMachineContext<String, String> context, WorkflowInstanceEntity contextObj)
                    throws Exception {
                var children = context.getChilds();
                String state = context.getState();
                String event = context.getEvent();
                var eventHeaders = context.getEventHeaders();
                var extendedState = context.getExtendedState();
                var historyStates = context.getHistoryStates();
                String id = context.getId();
                var stateMachineContext = new DefaultStateMachineContext<>(children, state, event, eventHeaders, extendedState, historyStates, id);
                contextObj.setStateMachineContext(stateMachineContext);
            }

            @SuppressWarnings("RedundantThrows")
            @Override
            public StateMachineContext<String, String> read(WorkflowInstanceEntity contextObj) throws Exception {
                return contextObj.getStateMachineContext();
            }
        };
    }

    /**
     * @return The DefaultStateMachinePersister which is an implementation of the StateMachinePersister interface, which is responsible for persisting and
     * restoring a state machine from a persistent storage.
     */
    @Bean
    public StateMachinePersister<String, String, WorkflowInstanceEntity> stateMachinePersister(
            @Autowired StateMachinePersist<String, String, WorkflowInstanceEntity> stateMachinePersist) {
        return new DefaultStateMachinePersister<>(stateMachinePersist);
    }

    /**
     * @return A bean of the DefaultStateMachineAdapter
     */
    @Bean
    public DefaultStateMachineAdapter<WorkflowInstanceEntity> stateMachineAdapter(
            @Autowired StateMachinePersister<String, String, WorkflowInstanceEntity> stateMachinePersister) {
        return new DefaultStateMachineAdapter<>(stateMachinePersister);
    }

}
