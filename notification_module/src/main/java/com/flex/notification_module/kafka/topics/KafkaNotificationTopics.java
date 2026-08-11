package com.flex.notification_module.kafka.topics;

public class KafkaNotificationTopics {
    public static final String NO_AGENT_IN_POINT_TOPIC = "/topic/notifications/no-agents-in-point/";
    public static final String JOB_TIMEOUT = "/topic/notifications/job-timeout/";

    //trigger topics
    public static final String CUSTOMER_ARRIVED_TOPIC = "/topic/trigger/customer-arrived/";
    public static final String JOB_SERVING_TOPIC = "/topic/trigger/job-serving/";
}
