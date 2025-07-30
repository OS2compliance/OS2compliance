ALTER TABLE threat_assessments
   ADD society BIT(1) NOT NULL DEFAULT FALSE,
   ADD authenticity BIT(1) NOT NULL DEFAULT FALSE,
   ADD inherited_confidentiality_society INT NULL,
   ADD inherited_integrity_society INT NULL,
   ADD inherited_availability_society INT NULL;

ALTER TABLE threat_assessment_responses
   ADD confidentiality_society INT NULL,
   ADD integrity_society INT NULL,
   ADD availability_society INT NULL,
   ADD authenticity_society INT NULL;

ALTER TABLE consequence_assessments
   ADD confidentiality_society INT NULL,
   ADD integrity_society INT NULL,
   ADD availability_society INT NULL;