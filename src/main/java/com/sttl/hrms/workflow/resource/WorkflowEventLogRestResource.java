package com.sttl.hrms.workflow.resource;

import com.sttl.hrms.workflow.data.model.entity.WorkflowEventLogEntity;
import com.sttl.hrms.workflow.resource.dto.WorkflowEventLogDto;
import com.sttl.hrms.workflow.service.WorkflowEventLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("wf/log")
@RequiredArgsConstructor
@Slf4j
@Validated
public class WorkflowEventLogRestResource {

    private final WorkflowEventLogService workflowEventLogService;

    @GetMapping("/")
    public @ResponseBody Map<String, List<WorkflowEventLogEntity>> getByWorkflowType(@RequestBody @Valid WorkflowEventLogDto eventLogDto) {
        return workflowEventLogService.getWorkflowEventLogsPartitionedByType(eventLogDto)
                .stream()
                .collect(Collectors.groupingBy(e -> e.getTypeId().getName()));
    }
}
