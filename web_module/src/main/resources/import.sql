insert into permissions(permission, employee) value ('Permit This', false);
insert into permissions(permission, employee) value ('Payments Verifier', false);
insert into permissions(permission, employee) value ('Role Management', false);
insert into permissions(permission, employee) value ('Permission Access', false);
insert into permissions(permission, employee) value ('User Management', false);
insert into permissions(permission, employee) value ('Decrypt Data', false);
insert into permissions(permission, employee) value ('Service Provider', false);
insert into permissions(permission, employee) value ('Centers Management', false);
insert into permissions(permission, employee) value ('Services Management', false);
insert into permissions(permission, employee) value ('Center Employee M.', false);
insert into permissions(permission, employee) value ('Points Management', false);
insert into permissions(permission, employee) value ('Cluster Management', false);
insert into permissions(permission, employee) value ('Holiday Management', false);
insert into permissions(permission, employee) value ('Notification Permission', false);
insert into permissions(permission, employee) value ('Notification Management', false);
insert into permissions(permission, employee) value ('Jobs Management', false);
insert into permissions(permission, employee) value ('Employee Management', false);
insert into permissions(permission, employee) value ('Assigned Clusters Management', false);

insert into permissions(permission, employee) value ('Employee Profile', true);
insert into permissions(permission, employee) value ('Employee Jobs', true);

# insert into roles(deleted, restricted, role, service_provider_id) value (false, false, 'Employee', 1);
#
# insert into role_permissions(permission_id, role_id) value (19, 2);
# insert into role_permissions(permission_id, role_id) value (20, 2);
# insert into role_permissions(permission_id, role_id) value (1, 2);
#
# insert into role_permission_access(add_permission, all_permission, assign_permission, delete_permission, get_all_permission, get_permission, update_permission, role_permission_id)
#     VALUE (false, true, false, false, false, false, false, 19);
# insert into role_permission_access(add_permission, all_permission, assign_permission, delete_permission, get_all_permission, get_permission, update_permission, role_permission_id)
#     VALUE (false, true, false, false, false, false, false, 20);
# insert into role_permission_access(add_permission, all_permission, assign_permission, delete_permission, get_all_permission, get_permission, update_permission, role_permission_id)
#     VALUE (false, true, false, false, false, false, false, 21);

insert into notification_type(deleted, type, name, description, crucial)
    value (false, 'GENERAL', 'General Notifications', 'This option enables general notification access.', false);
insert into notification_type(deleted, type, name, description, crucial)
    value (false, 'JOB_CREATED', 'Job Creation', 'This option notifies you every time a new job is created.', false);
insert into notification_type(deleted, type, name, description, crucial)
    value (false, 'NO_AGENT_FOR_JOB', 'No agent at point', 'This will notify the management when agent is arrived to service, but no agent is ready for the service.', true);