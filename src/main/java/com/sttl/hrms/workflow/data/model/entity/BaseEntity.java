package com.sttl.hrms.workflow.data.model.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.*;
import java.time.LocalDateTime;

@MappedSuperclass
@NoArgsConstructor
@Getter
@Setter
@ToString
@Slf4j
public abstract class BaseEntity {

    @Column(name = "company_id", nullable = false, updatable = false)
    private Long companyId;

    @Column(name = "branch_id", nullable = false, updatable = false)
    private Integer branchId;

    @Column(name = "create_by", updatable = false)
    private Long createdByUserId;

    @Column(name = "update_by")
    private Long updatedByUserId;

    @Column(name = "delete_by")
    private Long deletedByUserId;

    @Column(name = "create_date", updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "update_date")
    private LocalDateTime updatedDate;

    @Column(name = "delete_date")
    private LocalDateTime deletedDate;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdDate == null)
            this.createdDate = now;
        if (this.updatedDate == null)
            this.updatedDate = now;
        log.debug("{} entity: {}", "Saving", this);
    }

    @PreRemove
    public void preRemove() {
        this.deletedDate = LocalDateTime.now();
        log.debug("{} entity: {}", "Deleting", this);
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedDate = LocalDateTime.now();
        log.debug("{} entity: {}", "Updating", this);
    }
}
