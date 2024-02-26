alter table assets
    modify asset_status varchar(30) null;
alter table assets
    modify criticality varchar(30) null;

alter table registers
    modify status varchar(36) null;

