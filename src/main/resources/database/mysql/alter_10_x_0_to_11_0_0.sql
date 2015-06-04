create table o_qti_assessment_session (
   id bigint not null auto_increment,
   creationdate datetime not null,
   lastmodified datetime not null,
   q_exploded bit not null default 0,
   q_author_mode bit not null default 0,
   q_finish_time datetime,
   q_termination_time datetime,
   q_storage varchar(32),
   fk_identity bigint not null,
   fk_entry bigint not null,
   fk_course bigint,
   q_course_subident varchar(64),
   primary key (id)
);
alter table o_qti_assessment_session ENGINE = InnoDB;

alter table o_qti_assessment_session add constraint qti_sess_to_repo_entry_idx foreign key (fk_entry) references o_repositoryentry (repositoryentry_id);
alter table o_qti_assessment_session add constraint qti_sess_to_course_entry_idx foreign key (fk_course) references o_repositoryentry (repositoryentry_id);
alter table o_qti_assessment_session add constraint qti_sess_to_identity_idx foreign key (fk_identity) references o_bs_identity (id);


