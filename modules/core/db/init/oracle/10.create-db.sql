-- begin CEJT_CUSTOMER
create table CEJT_CUSTOMER (
    ID varchar2(32),
    CREATE_TS timestamp,
    CREATED_BY varchar2(50),
    VERSION number(10) not null,
    UPDATE_TS timestamp,
    UPDATED_BY varchar2(50),
    DELETE_TS timestamp,
    DELETED_BY varchar2(50),
    --
    NAME varchar2(255),
    --
    primary key (ID)
)^
-- end CEJT_CUSTOMER
