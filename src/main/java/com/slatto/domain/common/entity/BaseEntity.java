package com.slatto.domain.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 연관 테이블만 교체해 본체가 dirty 가 되지 않는 수정에서 updated_at 갱신을 강제한다.
     * updated_at 을 직접 건드려 dirty 로 만들면 UPDATE 가 발행되고, 최종 값은 @LastModifiedDate 가 채운다.
     */
    public void markUpdated() {
        this.updatedAt = LocalDateTime.now();
    }
}