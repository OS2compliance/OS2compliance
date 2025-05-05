alter table threat_catalog_threats
    drop foreign key FK_THREAT_CATALOG_THREATS_ON_THREAD_CATALOG_IDENTIFIER;

alter table threat_catalog_threats
    add constraint FK_THREAT_CATALOG_THREATS_ON_THREAD_CATALOG_IDENTIFIER
        foreign key (thread_catalog_identifier) references threat_catalogs (identifier)
            on delete cascade;
