package com.sttl.hrms.workflow.service;

import com.sttl.hrms.workflow.data.model.entity.LoanAppWorkflowInstanceEntity;
import com.sttl.hrms.workflow.data.model.entity.WorkflowInstanceEntity;
import com.sttl.hrms.workflow.data.model.repository.LoanAppWorkflowInstanceRepository;
import com.sttl.hrms.workflow.resource.dto.EventResponseDto;
import com.sttl.hrms.workflow.resource.dto.PassEventDto;
import com.sttl.hrms.workflow.statemachine.EventResultDto;
import com.sttl.hrms.workflow.statemachine.builder.StateMachineBuilder;
import com.sttl.hrms.workflow.statemachine.persist.StateMachineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.sttl.hrms.workflow.statemachine.builder.StateMachineBuilder.SMEvent.*;

@Service
@Slf4j
@Transactional(readOnly = true)
public class LoanAppService extends WorkflowService<LoanAppWorkflowInstanceEntity> {
    private final LoanAppWorkflowInstanceRepository loanAppRepository;

    public LoanAppService(StateMachineService<WorkflowInstanceEntity> stateMachineService, LoanAppWorkflowInstanceRepository loanAppRepository) {
        super(stateMachineService);
        this.loanAppRepository = loanAppRepository;
    }

    public boolean existsById(Long id) {
        return Optional.ofNullable(loanAppRepository.existsByIdAndWFType(id)).orElse(false);
    }

    @Transactional
    public LoanAppWorkflowInstanceEntity create(LoanAppWorkflowInstanceEntity entity) {
        LocalDateTime now = entity.getCreatedDate() == null ? LocalDateTime.now() : entity.getCreatedDate();
        Long userId = entity.getCreatedByUserId();
        entity.setCreatedDate(now);

        List<PassEventDto> passEvents = List.of(
                PassEventDto.builder().workflowType(entity.getTypeId()).event(E_CREATE.name()).actionBy(userId)
                        .actionDate(now).build(),
                PassEventDto.builder().workflowType(entity.getTypeId()).event(E_SUBMIT.name()).actionBy(userId)
                        .actionDate(now).build(),
                PassEventDto.builder().workflowType(entity.getTypeId()).event(E_TRIGGER_REVIEW_OF.name())
                        .actionBy(userId).actionDate(now).build(),
                PassEventDto.builder().workflowType(entity.getTypeId()).event(E_TRIGGER_FLOW_JUNCTION.name())
                        .actionBy(userId).actionDate(now).build()
        );
        return createApplication(loanAppRepository, entity, passEvents);
    }

    public LoanAppWorkflowInstanceEntity getById(Long id) {
        return getApplicationById(id, loanAppRepository);
    }

    public List<LoanAppWorkflowInstanceEntity> getAll() {
        return getAll(loanAppRepository);
    }

    @Transactional
    public List<EventResponseDto> passEventToSM(PassEventDto passEvent) {
        StateMachineBuilder.SMEvent smEvent = StateMachineBuilder.SMEvent.getByName(passEvent.getEvent());
        List<PassEventDto> passEvents = switch (smEvent) {
            case E_CREATE ->
                    PassEventDto.createPassEvents(passEvent, E_SUBMIT, E_TRIGGER_REVIEW_OF, E_TRIGGER_FLOW_JUNCTION);
            case E_SUBMIT -> PassEventDto.createPassEvents(passEvent, E_TRIGGER_REVIEW_OF, E_TRIGGER_FLOW_JUNCTION);
            case E_TRIGGER_REVIEW_OF -> PassEventDto.createPassEvents(passEvent, E_TRIGGER_FLOW_JUNCTION);
            case E_APPROVE, E_REJECT, E_CANCEL -> PassEventDto.createPassEvents(passEvent);
            case E_REQUEST_CHANGES_IN, E_TRIGGER_FLOW_JUNCTION, E_FORWARD, E_ROLL_BACK, E_TRIGGER_COMPLETE ->
                    List.of(passEvent);
        };
        return passEvents(passEvents, loanAppRepository);
    }

    @Transactional
    public void delete(Long id) {
        deleteApplication(id, loanAppRepository);
    }

    @Transactional
    public List<EventResultDto> resetStateMachine(PassEventDto passEvent) {
        return resetStateMachine(passEvent, loanAppRepository);
    }

}
