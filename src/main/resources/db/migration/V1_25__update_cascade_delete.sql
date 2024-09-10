alter table relation_properties
    drop foreign key FK_RELATION_PROPERTIES_ON_RELATION;

alter table relation_properties
    add constraint FK_RELATION_PROPERTIES_ON_RELATION
        foreign key (relation_id) references relations (id)
            on delete cascade;
