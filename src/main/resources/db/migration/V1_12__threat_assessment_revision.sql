ALTER TABLE threat_assessments ADD COLUMN next_revision datetime(6) null;
ALTER TABLE threat_assessments ADD COLUMN revision_interval varchar(100) null;
