package com.sttl.hrms.workflow.data.model.entity;

import com.sttl.hrms.workflow.data.enums.LoanType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "loan_wf_inst", schema = "public")
@Where(clause = "isActive = 1")
@NoArgsConstructor
@ToString(callSuper = true)
@Getter
@Setter
public class LoanAppWorkflowInstanceEntity extends WorkflowInstanceEntity {

    @Column(name = "is_active", nullable = false)
    private short isActive = 1;

    @Column(name = "loan_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private LoanType loanType;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        LoanAppWorkflowInstanceEntity that = (LoanAppWorkflowInstanceEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
