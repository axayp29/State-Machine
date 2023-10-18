package com.sttl.hrms.workflow.resource;

import com.sttl.hrms.workflow.data.model.entity.LeaveAppWorkFlowInstanceEntity;
import com.sttl.hrms.workflow.exception.WorkflowException;
import com.sttl.hrms.workflow.resource.dto.EventResponseDto;
import com.sttl.hrms.workflow.resource.dto.LeaveAppWFInstanceDto;
import com.sttl.hrms.workflow.resource.dto.PassEventDto;
import com.sttl.hrms.workflow.service.LeaveAppService;
import com.sttl.hrms.workflow.statemachine.EventResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@RestController
@RequestMapping("wf/leave")
@RequiredArgsConstructor
@Slf4j
@Validated
public class LeaveAppWFRestController {

    private final LeaveAppService leaveAppWFService;

    @PostMapping("/")
    @ResponseStatus(HttpStatus.CREATED)
    public @ResponseBody LeaveAppWorkFlowInstanceEntity createLeaveApp(@RequestBody @Valid LeaveAppWFInstanceDto dto) {
        log.debug("create leaveApp Entity from dto: {}", dto);
        if (dto.getCreatedByUserId() == null)
            throw new WorkflowException("Cannot create leave application", new IllegalArgumentException("Created By User Id cannot be null"));
        return leaveAppWFService.create(LeaveAppWFInstanceDto.toEntity(dto));
    }

    @GetMapping("/{id}")
    public @ResponseBody LeaveAppWorkFlowInstanceEntity getLeaveAppById(@PathVariable("id") Long id) {
        if (!leaveAppWFService.existsById(id))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return leaveAppWFService.getById(id);
    }

    @GetMapping("/")
    public @ResponseBody List<LeaveAppWorkFlowInstanceEntity> getAllLeaveApps() {
        return leaveAppWFService.getAll();
    }

    @PostMapping("/event")
    public @ResponseBody List<EventResponseDto> sendEventToLeaveAppWF(@NotNull @RequestBody @Valid PassEventDto eventDto) {
        log.debug("update leaveApp Entity with event: {}", eventDto);
        return leaveAppWFService.passEventToSM(eventDto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLeaveApp(@PathVariable("id") Long id) {
        if (!leaveAppWFService.existsById(id))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        leaveAppWFService.delete(id);
    }

    @PostMapping("/reset")
    public @ResponseBody List<EventResultDto> resetWorkFlow(@NotNull @RequestBody @Valid PassEventDto eventDto) {
        return leaveAppWFService.resetStateMachine(eventDto);
    }

}
