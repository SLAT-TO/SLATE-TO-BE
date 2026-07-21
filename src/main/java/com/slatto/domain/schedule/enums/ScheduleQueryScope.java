package com.slatto.domain.schedule.enums;

public enum ScheduleQueryScope {
    ALL, PERSONAL, PROJECT;

    public ScheduleScope toScheduleScope() {
        return this == ALL ? null : ScheduleScope.valueOf(name());
    }
}
