
alter table incident_field_responses
    add answer_choice_values text null after answer_element_ids;
alter table incidents
    add created_by_uuid varchar(16) null after version;
alter table incident_fields
    change index_column index_column_name varchar(255) null;
alter table incident_field_responses
    add index_column_name varchar(255) null after incident_type;

alter table incident_field_responses
    drop foreign key FK_INCIDENT_FIELD_RESPONSES_ON_INCIDENT;
alter table incident_field_responses
    add constraint FK_INCIDENT_FIELD_RESPONSES_ON_INCIDENT
        foreign key (incident_id) references incidents (id)
            on delete cascade;

