package com.flex.user_module.events;

import com.flex.job_module.impl.entities.Job;
import com.flex.service_module.impl.entities.ServicePoint;

public record AgentLoginEvent(ServicePoint servicePoint, Job job) {
}
