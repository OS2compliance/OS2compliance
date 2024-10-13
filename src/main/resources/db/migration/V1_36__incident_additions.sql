
alter table incident_field_responses
    add answer_choice_values text null after answer_element_ids;
alter table incidents
    add created_by_uuid varchar(16) null after version;
alter table incident_fields
    change index_column index_column_name varchar(255) null;
