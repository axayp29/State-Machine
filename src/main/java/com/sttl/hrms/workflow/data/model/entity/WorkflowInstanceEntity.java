package com.sttl.hrms.workflow.data.model.entity;

import com.sttl.hrms.workflow.data.Pair;
import com.sttl.hrms.workflow.data.enums.WorkflowType;
import com.sttl.hrms.workflow.data.model.converter.WorkflowTypeIdConverter;
import com.sttl.hrms.workflow.statemachine.builder.StateMachineBuilder;
import com.sttl.hrms.workflow.statemachine.persist.StateMachineContextConverter;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.springframework.statemachine.support.DefaultStateMachineContext;

import javax.persistence.*;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "wf_inst_mst", schema = "public")
@Inheritance(strategy = InheritanceType.JOINED)
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@NoArgsConstructor
@Getter
@Setter
@ToString(callSuper = true)
@Slf4j
public abstract class WorkflowInstanceEntity extends BaseEntity implements ContextEntity {

    @Id
    @Column(nullable = false, unique = true, updatable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "WF_INST_SEQ")
    @SequenceGenerator(name = "WF_INST_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "type_id", nullable = false, updatable = false)
    @Convert(converter = WorkflowTypeIdConverter.class)
    private WorkflowType typeId;

    @Column(name = "roll_back_count")
    private short timesRolledBackCount;

    @Column(name = "return_count")
    private short timesReturnedCount;

    @Column(name = "`version`", nullable = false)
    private short workflowVersion;

    @Column(columnDefinition = "jsonb", name = "reviewers")
    @Type(type = "jsonb")
    @Basic(fetch = FetchType.EAGER)
    private List<Pair<Integer, Set<Long>>> reviewers = Collections.emptyList();

    @Column(name = "current_state", length = 100)
    private String currentState = StateMachineBuilder.SMState.S_INITIAL.name();

    @Convert(converter = StateMachineContextConverter.class)
    @Column(name = "statemachine", columnDefinition = "bytea")
    @ToString.Exclude
    private DefaultStateMachineContext<String, String> stateMachineContext;

    @Column(name = "statemachine_id", nullable = false, length = 100)
    private String stateMachineId;

    @Column(name = "fwd_count", nullable = false, columnDefinition = "int2 default 0")
    private short forwardCount = 0;

    @Column(name = "last_fwd_by")
    private Long lastForwardedBy;

    public void setStateMachineContext(DefaultStateMachineContext<String, String> stateMachineContext) {
        this.setCurrentState(stateMachineContext.getState());
        this.stateMachineContext = stateMachineContext;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        WorkflowInstanceEntity that = (WorkflowInstanceEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
