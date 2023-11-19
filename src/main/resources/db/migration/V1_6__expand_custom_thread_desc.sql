
ALTER TABLE custom_threats MODIFY description TEXT;
ALTER TABLE custom_threats MODIFY threat_type varchar(1024);

ALTER TABLE threat_assessment_responses MODIFY problem varchar(2048) null;
ALTER TABLE threat_assessment_responses MODIFY existing_measures varchar(2048) null;
ALTER TABLE threat_assessment_responses MODIFY elaboration varchar(2048) null;
