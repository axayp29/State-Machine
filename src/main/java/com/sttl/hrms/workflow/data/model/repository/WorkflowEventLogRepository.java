package com.sttl.hrms.workflow.data.model.repository;

import com.sttl.hrms.workflow.data.model.entity.WorkflowEventLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkflowEventLogRepository extends JpaRepository<WorkflowEventLogEntity, Long> {

}