package com.sttl.hrms.workflow.resource;

import com.sttl.hrms.workflow.data.model.entity.LoanAppWorkflowInstanceEntity;
import com.sttl.hrms.workflow.exception.WorkflowException;
import com.sttl.hrms.workflow.resource.dto.EventResponseDto;
import com.sttl.hrms.workflow.resource.dto.LoanAppWFInstanceDto;
import com.sttl.hrms.workflow.resource.dto.PassEventDto;
import com.sttl.hrms.workflow.service.LoanAppService;
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
@RequestMapping("wf/loan")
@RequiredArgsConstructor
@Slf4j
@Validated
public class LoanAppWFRestController {

    private final LoanAppService loanAppWFService;

    @PostMapping("/")
    @ResponseStatus(HttpStatus.CREATED)
    public @ResponseBody LoanAppWorkflowInstanceEntity createLoanApp(@RequestBody @Valid LoanAppWFInstanceDto dto) {
        log.debug("create LoanApp Entity from dto: {}", dto);
        if (dto.getCreatedByUserId() == null)
            throw new WorkflowException("Cannot create loan application", new IllegalArgumentException("Created By User Id cannot be null"));
        return loanAppWFService.create(LoanAppWFInstanceDto.toEntity(dto));
    }

    @GetMapping("/{id}")
    public @ResponseBody LoanAppWorkflowInstanceEntity getLoanAppById(@PathVariable("id") Long id) {
        if (!loanAppWFService.existsById(id))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return loanAppWFService.getById(id);
    }

    @GetMapping("/")
    public @ResponseBody List<LoanAppWorkflowInstanceEntity> getAllLoanApp() {
        return loanAppWFService.getAll();
    }

    @PostMapping("/event")
    public @ResponseBody List<EventResponseDto> sendEventToLoanAppWF(@NotNull @RequestBody @Valid PassEventDto eventDto) {
        log.debug("update loanApp with event: {}", eventDto);
        return loanAppWFService.passEventToSM(eventDto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLoanApp(@PathVariable("id") Long id) {
        if (!loanAppWFService.existsById(id))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        loanAppWFService.delete(id);
    }

    @PostMapping("/reset")
    public @ResponseBody List<EventResultDto> resetWorkFlow(@NotNull @RequestBody @Valid PassEventDto eventDto) {
        return loanAppWFService.resetStateMachine(eventDto);
    }

}
