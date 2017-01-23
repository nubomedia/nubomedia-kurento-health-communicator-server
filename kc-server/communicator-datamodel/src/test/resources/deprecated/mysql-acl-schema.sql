create table acl_sid (
    id bigint unsigned not null auto_increment primary key,
    principal tinyint(1) not null,
    sid varchar(100) not null,
    unique index acl_sid_idx_1 (sid, principal)
) engine = InnoDb;

create table acl_class (
    id bigint unsigned not null auto_increment primary key,
    class varchar(100) unique not null
) engine = InnoDb;

create table acl_object_identity (
    id bigint unsigned not null auto_increment primary key,
    object_id_class bigint unsigned not null,
    object_id_identity bigint unsigned not null,
    parent_object bigint unsigned,
    owner_sid bigint unsigned,
    entries_inheriting tinyint(1) not null,
    unique index acl_object_identity_idx_1
        (object_id_class, object_id_identity),
    foreign key (object_id_class) references acl_class (id),
    foreign key (parent_object) references acl_object_identity (id),
    foreign key (owner_sid) references acl_sid (id)
) engine = InnoDb;

create table acl_entry (
    id bigint unsigned not null auto_increment primary key,
    acl_object_identity bigint unsigned not null,
    ace_order int unsigned not null,
    sid bigint unsigned not null,
    mask int not null,
    granting tinyint(1) not null,
    audit_success tinyint(1) not null,
    audit_failure tinyint(1) not null,
    unique index acl_entry_idx_1 (acl_object_identity, ace_order),
    foreign key (acl_object_identity)
        references acl_object_identity (id),
    foreign key (sid) references acl_sid (id)
) engine = InnoDb;