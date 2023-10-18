package com.sttl.hrms.workflow.statemachine.persist;

import com.sttl.hrms.workflow.data.Pair;
import com.sttl.hrms.workflow.data.model.entity.WorkflowTypeEntity;
import com.sttl.hrms.workflow.statemachine.builder.StateMachineBuilderFactory;
import com.sttl.hrms.workflow.statemachine.exception.StateMachineException;
import com.sttl.hrms.workflow.statemachine.exception.StateMachinePersistenceException;
import com.sttl.hrms.workflow.statemachine.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.persist.StateMachinePersister;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * This is a wrapper class over the DefaultStateMachinePersister.
 * We use its methods to persist or restore a state machine context from and to a state machine.
 *
 * @param <T> Parameter for the context object class which provides the required StateMachineContext
 */
@Slf4j
@Transactional
@RequiredArgsConstructor
public class DefaultStateMachineAdapter<T> {

    private final StateMachinePersister<String, String, T> stateMachinePersister;

    public StateMachine<String, String> restore(StateMachine<String, String> stateMachine, T contextObj) {
        try {
            StateMachine<String, String> restoredStateMachine = stateMachinePersister.restore(stateMachine, contextObj);
            log.debug("Restored stateMachine: {}", StringUtil.stateMachine(restoredStateMachine, false));
            log.trace("Restored stateMachine with details: {}", StringUtil.stateMachine(restoredStateMachine, true));
            return restoredStateMachine;
        } catch (Exception e) {
            String errMsg = "Could not restore a state machine context from the database, for a statemachine with id: " +
                    stateMachine.getId() + " and statemachine context: " + contextObj;
            throw new StateMachinePersistenceException(errMsg, e);
        }
    }

    public void persist(StateMachine<String, String> stateMachine, T contextObj) {
        try {
            stateMachinePersister.persist(stateMachine, contextObj);
            log.debug("Persisted stateMachine: {} for entity: {}", StringUtil.stateMachine(stateMachine, false), contextObj);
        } catch (Exception e) {
            String errMsg = "Could not save to the database the state machine context: " +
                    contextObj + " for statemachine with id: " + stateMachine.getId();
            throw new StateMachinePersistenceException(errMsg, e);
        }
    }

    public StateMachine<String, String> createStateMachine(WorkflowTypeEntity typeEntity,
            List<Pair<Integer, Set<Long>>> reviewers, Long createdBy) {
        try {
            StateMachine<String, String> stateMachine =
                    StateMachineBuilderFactory.getStateMachineFromEntityAndType(typeEntity, reviewers, createdBy);
            log.debug("Created and started stateMachine: {}", StringUtil.stateMachine(stateMachine, false));
            return stateMachine;
        } catch (Exception e) {
            String errMsg = "Could not create a new statemachine from the StateMachineBuilderFactory with machineId: " + typeEntity.getTypeId();
            throw new StateMachineException(errMsg, e);
        }
    }

}
