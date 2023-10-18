package com.sttl.hrms.workflow.service;

import com.sttl.hrms.workflow.data.model.entity.LeaveAppWorkFlowInstanceEntity;
import com.sttl.hrms.workflow.data.model.entity.WorkflowInstanceEntity;
import com.sttl.hrms.workflow.data.model.repository.LeaveAppWorkflowInstanceRepository;
import com.sttl.hrms.workflow.resource.dto.EventResponseDto;
import com.sttl.hrms.workflow.resource.dto.PassEventDto;
import com.sttl.hrms.workflow.statemachine.EventResultDto;
import com.sttl.hrms.workflow.statemachine.builder.StateMachineBuilder.SMEvent;
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
public class LeaveAppService extends WorkflowService<LeaveAppWorkFlowInstanceEntity> {
    private final LeaveAppWorkflowInstanceRepository leaveAppRepository;

    public LeaveAppService(StateMachineService<WorkflowInstanceEntity> stateMachineService, LeaveAppWorkflowInstanceRepository leaveAppRepository) {
        super(stateMachineService);
        this.leaveAppRepository = leaveAppRepository;
    }

    public boolean existsById(Long id) {
        return Optional.ofNullable(leaveAppRepository.existsByIdAndWFType(id)).orElse(false);
    }

    @Transactional
    public LeaveAppWorkFlowInstanceEntity create(LeaveAppWorkFlowInstanceEntity entity) {
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
        return createApplication(leaveAppRepository, entity, passEvents);
    }

    public LeaveAppWorkFlowInstanceEntity getById(Long id) {
        return getApplicationById(id, leaveAppRepository);
    }

    public List<LeaveAppWorkFlowInstanceEntity> getAll() {
        return getAll(leaveAppRepository);
    }

    @Transactional
    public List<EventResponseDto> passEventToSM(PassEventDto passEvent) {
        SMEvent smEvent = SMEvent.getByName(passEvent.getEvent());
        List<PassEventDto> passEvents = switch (smEvent) {
            case E_CREATE -> PassEventDto.createPassEvents(passEvent, E_SUBMIT, E_TRIGGER_REVIEW_OF,
                    E_TRIGGER_FLOW_JUNCTION);
            case E_SUBMIT -> PassEventDto.createPassEvents(passEvent, E_TRIGGER_REVIEW_OF, E_TRIGGER_FLOW_JUNCTION);
            case E_TRIGGER_REVIEW_OF -> PassEventDto.createPassEvents(passEvent, E_TRIGGER_FLOW_JUNCTION);
            case E_APPROVE, E_REJECT, E_CANCEL -> PassEventDto.createPassEvents(passEvent);
            case E_REQUEST_CHANGES_IN, E_TRIGGER_FLOW_JUNCTION, E_FORWARD, E_ROLL_BACK, E_TRIGGER_COMPLETE ->
                    List.of(passEvent);
        };
        return passEvents(passEvents, leaveAppRepository);
    }

    @Transactional
    public void delete(Long id) {
        deleteApplication(id, leaveAppRepository);
    }

    @Transactional
    public List<EventResultDto> resetStateMachine(PassEventDto passEventDto) {
        return resetStateMachine(passEventDto, leaveAppRepository);
    }

}
