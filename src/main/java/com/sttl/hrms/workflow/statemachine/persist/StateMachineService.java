package com.sttl.hrms.workflow.statemachine.persist;


import com.sttl.hrms.workflow.data.Pair;
import com.sttl.hrms.workflow.data.model.entity.WorkflowInstanceEntity;
import com.sttl.hrms.workflow.data.model.entity.WorkflowTypeEntity;
import com.sttl.hrms.workflow.resource.dto.PassEventDto;
import com.sttl.hrms.workflow.resource.dto.WorkflowEventLogDto;
import com.sttl.hrms.workflow.service.WorkflowEventLogService;
import com.sttl.hrms.workflow.service.WorkflowTypeService;
import com.sttl.hrms.workflow.statemachine.EventResultDto;
import com.sttl.hrms.workflow.statemachine.builder.Actions;
import com.sttl.hrms.workflow.statemachine.exception.StateMachineException;
import com.sttl.hrms.workflow.statemachine.util.ExtStateUtil;
import com.sttl.hrms.workflow.statemachine.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.StateMachine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.*;

import static com.sttl.hrms.workflow.statemachine.SMConstants.KEY_ADMIN_IDS;
import static org.springframework.statemachine.StateMachineEventResult.ResultType.ACCEPTED;

@Service
@RequiredArgsConstructor
@Slf4j
public class StateMachineService<T extends WorkflowInstanceEntity> {

    private final DefaultStateMachineAdapter<T> stateMachineAdapter;
    private final WorkflowTypeService workflowTypeService;
    private final WorkflowEventLogService workflowEventLogService;

    @Transactional(readOnly = true)
    public StateMachine<String, String> createStateMachine(@NotNull T entity) {

        WorkflowTypeEntity typeEntity = workflowTypeService.findByTypeId(entity.getTypeId());

        // create statemachine as per the entity's statemachine id.
        var stateMachine = Optional
                .ofNullable(stateMachineAdapter.createStateMachine(typeEntity, entity.getReviewers(), entity.getCreatedByUserId()))
                .orElseThrow(() -> new StateMachineException("StateMachine was not created"));

        // set the state machine extended state from the workflow type and workflow instance
        List<Pair<Integer, Set<Long>>> reviewersList = entity.getReviewers();
        var properties = workflowTypeService.getWorkFlowPropertiesByType(entity.getTypeId());
        Map<Integer, Set<Long>> reviewerMap = new LinkedHashMap<>(Pair.pairListToMap(reviewersList));
        Actions.initial(stateMachine, properties, reviewerMap, entity.getCreatedByUserId());
        return stateMachine;
    }

    @Transactional
    public void saveStateMachineToEntity(@NotNull StateMachine<String, String> stateMachine, @NotNull T entity, List<EventResultDto> eventResultList, boolean logWorkflowEvent) {
        stateMachineAdapter.persist(stateMachine, entity);
        log.debug("persisted stateMachine context: {} for entity with Id: {}", entity.getStateMachineContext(), entity.getId());
        if (logWorkflowEvent)
            writeToLog(entity, stateMachine, eventResultList);
    }

    @Transactional(readOnly = true)
    public StateMachine<String, String> getStateMachineFromEntity(T entity) {
        WorkflowTypeEntity typeEntity = workflowTypeService.findByTypeId(entity.getTypeId());
        var stateMachine = stateMachineAdapter.restore(stateMachineAdapter.createStateMachine(typeEntity,
                entity.getReviewers(), entity.getCreatedByUserId()), entity);
        log.debug("For entity with id: {} and currentState: {}, Restored statemachine: {}",
                entity.getId(), entity.getCurrentState(), StringUtil.stateMachine(stateMachine, false));
        return stateMachine;
    }

    public void writeToLog(T entity, StateMachine<String, String> stateMachine, List<EventResultDto> eventResultList) {
        for (EventResultDto result : eventResultList) {
            // log the event asynchronously once it is successfully processed by the statemachine.
            var wfEventLogDto = WorkflowEventLogDto.builder()
                    .companyId(entity.getCompanyId())
                    .branchId(entity.getBranchId())
                    .typeId(entity.getTypeId().getTypeId())
                    .instanceId(entity.getId())
                    .state(result.getCurrentState())
                    .event(result.getEvent())
                    .actionDate(LocalDateTime.now())
                    .completed((short) (stateMachine.isComplete() ? 1 : 0))
                    .actionBy(result.getActionBy())
                    .userRole((short) 0) //TODO
                    .comment(result.getComment())
                    .build();
            workflowEventLogService.logEvent(wfEventLogDto);
        }
    }


    @SuppressWarnings("unchecked")
    @Transactional
    public List<EventResultDto> resetStateMachine(T entity, PassEventDto passEventDto, StateMachine<String, String> stateMachine) {
        List<Long> adminIds = (List<Long>) ExtStateUtil.get(stateMachine.getExtendedState(), KEY_ADMIN_IDS, List.class,
                Collections.emptyList());
        if (adminIds.contains(passEventDto.getActionBy())) {
            throw new StateMachineException("Cannot reset workflow. UserId is not present in list of accepted admin " +
                    "Ids.");
        }
        stateMachine.stopReactively().block();
        return List.of(EventResultDto.builder()
                .currentState(stateMachine.getState().getId())
                .isSubMachine(false)
                .resultType(ACCEPTED)
                .comment(Optional.ofNullable(passEventDto.getComment()).orElse("Reset by Admin"))
                .event("RESET")
                .isComplete(false)
                .build());
    }

}
