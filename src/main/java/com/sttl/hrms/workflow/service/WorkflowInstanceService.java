package com.sttl.hrms.workflow.service;

import com.sttl.hrms.workflow.data.enums.WorkflowType;
import com.sttl.hrms.workflow.data.model.entity.WorkflowInstanceEntity;
import com.sttl.hrms.workflow.data.model.repository.WorkflowInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class WorkflowInstanceService {

    private final WorkflowInstanceRepository workflowInstanceRepository;

    public WorkflowInstanceEntity findById(Long id) {
        var entity = workflowInstanceRepository.findById(id).orElse(null);
        log.debug("found workflowInstanceEntity: {} by Id: {}", entity, id);
        return entity;
    }

    public List<WorkflowInstanceEntity> findByCompanyIdAndBranchId(Long companyId, Integer branchId) {
        var entityList = workflowInstanceRepository.findByCompanyIdAndBranchId(companyId, branchId);
        log.debug("found workflowInstanceEntityList: {} by companyId: {} and branchId: {}", entityList, companyId, branchId);
        return entityList;
    }

    public List<WorkflowInstanceEntity> findByTypeId(Long companyId, Integer branchId, Integer typeId) {
        WorkflowType type = WorkflowType.fromId(typeId);
        var entityList = workflowInstanceRepository.findByTypeId(companyId, branchId, type);
        log.debug("found workflowInstanceEntityList: {} by companyId: {}, branchId: {} and typeId: {}", entityList, companyId, branchId, type);
        return entityList;
    }
}
