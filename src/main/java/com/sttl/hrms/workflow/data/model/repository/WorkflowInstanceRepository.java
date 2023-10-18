package com.sttl.hrms.workflow.data.model.repository;

import com.sttl.hrms.workflow.data.enums.WorkflowType;
import com.sttl.hrms.workflow.data.model.entity.WorkflowInstanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
//TODO: optimize query: currently creates left outer join on each read query
public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstanceEntity, Long> {

    @Query("SELECT wf "
            + "FROM WorkflowInstanceEntity wf "
            + "INNER JOIN WorkflowTypeEntity wft "
            + "     ON wft.typeId = wf.typeId "
            + "WHERE wft.isActive = 1 "
            + "     AND wf.id = :id")
    Optional<WorkflowInstanceEntity> findById(@Param("id") Long id);

    @Query(value = " SELECT wf "
            + " FROM WorkflowInstanceEntity wf "
            + " WHERE wf.companyId = :companyId "
            + "     AND wf.branchId = :branchId")
    List<WorkflowInstanceEntity> findByCompanyIdAndBranchId(@Param("companyId") Long companyId, @Param("branchId") Integer branchId);

    @Query(value = " SELECT wf "
            + " FROM WorkflowInstanceEntity wf "
            + " WHERE wf.companyId = :companyId "
            + "     AND wf.branchId = :branchId"
            + "     AND wf.typeId = :typeId")
    List<WorkflowInstanceEntity> findByTypeId(@Param("companyId") Long companyId, @Param("branchId") Integer branchId,
            @Param("typeId") WorkflowType typeId);

}
