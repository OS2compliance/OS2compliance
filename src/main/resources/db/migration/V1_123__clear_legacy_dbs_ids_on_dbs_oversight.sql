-- dbs_id paa raekker fra den gamle DBS-integration er gamle Document-id'er, som ligger i samme
-- talrum som den nye platforms audit-id'er. Platform-syncen matcher paa dbs_id og kaprede derfor
-- gamle dokument-raekker naar et audit-id kolliderede: navn og audit_link blev overskrevet, mens
-- leverandoeren forblev den gamle - hvorefter auditens link fannede ud til en forkert leverandoers
-- opgaver (set hos Kalundborg 6/8-2026: EasyIQ-audit under Gyldendal, Plan2learn-audit under itm8).
--
-- Raekker uden audit_link er aldrig roert af platform-syncen (den saetter altid linket), saa deres
-- dbs_id er et gammelt dokument-id uden fremtidig vaerdi - den gamle integration er nedlagt.
-- Nulles den, kan raekkerne ikke laengere kapres; cutover-adoption via navn+leverandoer virker
-- fortsat og saetter et korrekt audit-id ved naeste match.
--
-- dbs_id er NOT NULL fra V1_32 og skal goeres nullable foerst. Entiteten har ingen
-- nullable=false, og synkroniseringen matcher null-sikkert (Objects.equals).
ALTER TABLE dbs_oversight MODIFY COLUMN dbs_id BIGINT NULL;
UPDATE dbs_oversight SET dbs_id = NULL WHERE audit_link IS NULL;
