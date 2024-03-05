
alter table assets add column dpia_opt_out BIT NOT NULL DEFAULT b'0';
alter table assets add column threat_assessment_opt_out BIT NOT NULL DEFAULT b'0';
alter table assets add column threat_assessment_opt_out_reason TEXT default null;
