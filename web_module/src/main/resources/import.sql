insert into permissions(permission) value ('Permit This');
insert into permissions(permission) value ('Payments Verifier');
insert into permissions(permission) value ('Role Management');
insert into permissions(permission) value ('Permission Access');
insert into permissions(permission) value ('User Management');
insert into permissions(permission) value ('Decrypt Data');
insert into permissions(permission) value ('Service Provider');
insert into permissions(permission) value ('Centers Management');
insert into permissions(permission) value ('Services Management');
insert into permissions(permission) value ('Center Employee M.');
insert into permissions(permission) value ('Points Management');
insert into permissions(permission) value ('Cluster Management');
insert into permissions(permission) value ('Holiday Management');
insert into permissions(permission) value ('Notification Permission');
insert into permissions(permission) value ('Notification Management');
insert into permissions(permission) value ('Jobs Management');
insert into permissions(permission) value ('Employee Management');
insert into permissions(permission) value ('Assigned Clusters Management');

insert into notification_type(deleted, type, name, description)
    value (false, 'JOB_CREATED', 'Job Creation', 'This option notifies you every time a new job is created.');
