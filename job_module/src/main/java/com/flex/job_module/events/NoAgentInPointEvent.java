package com.flex.job_module.events;

import com.flex.job_module.impl.entities.Job;

public record NoAgentInPointEvent(Integer pointId, Job job) {
}
