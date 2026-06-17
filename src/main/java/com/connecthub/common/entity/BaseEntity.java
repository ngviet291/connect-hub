package com.connecthub.common.entity;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity {

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}