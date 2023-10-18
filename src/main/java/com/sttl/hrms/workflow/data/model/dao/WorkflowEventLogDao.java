package com.sttl.hrms.workflow.data.model.dao;

import com.sttl.hrms.workflow.data.enums.WorkflowType;
import com.sttl.hrms.workflow.data.model.entity.WorkflowEventLogEntity;
import com.sttl.hrms.workflow.exception.WorkflowException;
import com.sttl.hrms.workflow.resource.dto.WorkflowEventLogDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class WorkflowEventLogDao {

    private final JdbcTemplate jdbcTemplate;

    private static final String WF_LOG_TABLE_NAME = "wf_status_log";
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public List<WorkflowEventLogEntity> getWorkflowEventLogs(WorkflowEventLogDto wf) {
        String query = "SELECT * " +
                " FROM " + WF_LOG_TABLE_NAME + " " +
                " WHERE company_id = " + wf.getCompanyId() + " " +
                " AND branch_id = " + wf.getBranchId() + " " +
                Optional.ofNullable(wf.getTypeId()).map(typeId -> " AND type_id = " + typeId).orElse("") +
                Optional.ofNullable(wf.getInstanceId()).map(iid -> " AND instance_id = " + iid).orElse("") +
                Optional.ofNullable(wf.getActionDate()).map(DTF::format)
                        .map(ad -> " AND action_date > '" + ad + "'::date").orElse("") +
                Optional.ofNullable(wf.getState()).map(st -> " AND state = '" + st + "'").orElse("") +
                Optional.ofNullable(wf.getEvent()).map(e -> " AND event = '" + e + "'").orElse("") +
                Optional.ofNullable(wf.getActionBy()).map(by -> " AND action_by = " + by).orElse("") +
                Optional.ofNullable(wf.getUserRole()).map(ur -> " AND user_role = " + ur).orElse("") +
                Optional.ofNullable(wf.getCompleted()).map(c -> " AND completed = " + c).orElse("") +
                " ORDER BY instance_id DESC, action_date DESC " +
                " LIMIT 100 ";

        final String partitionTableName = Optional
                .ofNullable(wf.getTypeId())
                .map(WorkflowType::fromId)
                .map(WorkflowType::getTableName)
                .orElse(null);
        final String partitionAwareQuery = (partitionTableName == null) ? query : query.replace(WF_LOG_TABLE_NAME, partitionTableName);
        log.debug("workflow event log query: {}", partitionAwareQuery);
        try {
            return jdbcTemplate.query(partitionAwareQuery, (rs, rowNum) -> mapWorkflowEventLogEntityFromResultSet(rs));
        } catch (DataAccessException ex) {
            throw new WorkflowException(ex);
        }
    }

    private static WorkflowEventLogEntity mapWorkflowEventLogEntityFromResultSet(ResultSet rs) throws SQLException {
        return WorkflowEventLogEntity
                .builder()
                .id(rs.getLong("id"))
                .companyId(rs.getLong("company_id"))
                .branchId(rs.getInt("branch_id"))
                .typeId(WorkflowType.fromId(rs.getInt("type_id")))
                .instanceId(rs.getLong("instance_id"))
                .actionDate(rs.getTimestamp("action_date").toLocalDateTime())
                .state(rs.getString("state"))
                .event(rs.getString("event"))
                .actionBy(rs.getLong("action_by"))
                .userRole(rs.getShort("user_role"))
                .completed(rs.getShort("completed"))
                .build();
    }

}
