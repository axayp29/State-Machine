
CREATE TABLE IF NOT EXISTS public.wf_type_mst (
    id int8 NOT NULL,
    branch_id int4 NOT NULL,
    company_id int8 NOT NULL,
    "name" varchar(100) NOT NULL,
    type_id int4 NOT NULL,
    wef_date timestamp NOT NULL,
    properties jsonb NULL,
    is_active int2 NULL,
    create_by int8 NULL,
    update_by int8 NULL,
    delete_by int8 NULL,
    create_date timestamp NULL,
    update_date timestamp NULL,
    delete_date timestamp NULL,
    CONSTRAINT wf_type_mst_pkey PRIMARY KEY (id)
);

CREATE TABLE public.wf_inst_mst (
    id int8 NOT NULL,
    branch_id int4 NOT NULL,
    company_id int8 NOT NULL,
    type_id int4 NOT NULL,
    statemachine_id varchar(100) NOT NULL,
    "version" int2 NOT NULL,
    current_state varchar(100) NULL,
    statemachine bytea NULL,
    reviewers jsonb NULL,
    return_count int2 NULL,
    roll_back_count int2 NULL,
    create_by int8 NULL,
    update_by int8 NULL,
    delete_by int8 NULL,
    create_date timestamp NULL,
    update_date timestamp NULL,
    delete_date timestamp NULL,
    fwd_count int2 NOT NULL DEFAULT 0,
    last_fwd_by int8 NULL,
    CONSTRAINT wf_inst_mst_pk PRIMARY KEY (id),
    CONSTRAINT wf_inst_mst_un UNIQUE (company_id, branch_id, id, type_id)
);

CREATE TABLE IF NOT EXISTS public.leave_wf_inst (
    id int8 NOT NULL,
    leave_type varchar(100) NOT NULL,
    is_active int2 NOT NULL,
    CONSTRAINT leave_wf_inst_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.loan_wf_inst (
    id int8 NOT NULL,
    loan_type varchar(100) NOT NULL,
    is_active int2 NOT NULL,
    CONSTRAINT loan_wf_inst_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.wf_status_log (
    id int8 NOT NULL,
    action_by int8 NOT NULL,
    action_date timestamp NOT NULL,
    branch_id int4 NOT NULL,
    company_id int8 NOT NULL,
    completed int2 NOT NULL,
    "event" varchar(255) NOT NULL,
    instance_id int8 NOT NULL,
    state varchar(255) NOT NULL,
    type_id int4 NOT NULL,
    user_role int2 NOT NULL,
    "comment" varchar(1024) NULL,
    CONSTRAINT wf_status_log_pkey PRIMARY KEY (id, type_id)
) PARTITION BY LIST(type_id);

CREATE TABLE IF NOT EXISTS public.leaveapp_wf_status_log PARTITION OF public.wf_status_log FOR VALUES IN (1);

CREATE TABLE IF NOT EXISTS public.loanapp_wf_status_log PARTITION OF public.wf_status_log FOR VALUES IN (2);

CREATE INDEX leave_wf_inst_leave_type_idx ON public.leave_wf_inst (leave_type);

CREATE INDEX loan_wf_inst_loan_type_idx ON public.loan_wf_inst (loan_type);

CREATE INDEX wf_inst_mst_type_id_idx ON public.wf_inst_mst (company_id, branch_id, type_id);
CREATE INDEX wf_inst_mst_company_id_idx ON public.wf_inst_mst (company_id, branch_id);

CREATE INDEX wf_status_log_instance_id_idx ON public.wf_status_log (company_id, branch_id, instance_id);
CREATE INDEX wf_status_log_type_id_idx ON public.wf_status_log (company_id, branch_id, type_id);

CREATE INDEX wf_type_mst_company_id_idx ON public.wf_type_mst (company_id, branch_id, is_active);
CREATE INDEX wf_type_mst_type_id_idx ON public.wf_type_mst (type_id);

CREATE SEQUENCE public.hibernate_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 5;

CREATE SEQUENCE public.wf_inst_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 5;

CREATE SEQUENCE public.wf_log_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 5;

CREATE SEQUENCE public.wf_type_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 5;

--SELECT pg_catalog.setval('public.hibernate_sequence', 0, false);

--SELECT pg_catalog.setval('public.wf_inst_seq', 0, true);

--SELECT pg_catalog.setval('public.wf_log_seq', 0, true);

--SELECT pg_catalog.setval('public.wf_type_seq', 0, true);

-- ALTER TABLE ONLY public.leave_wf_inst
--    ADD CONSTRAINT leave_wf_inst_pkey PRIMARY KEY (id);

-- ALTER TABLE ONLY public.wf_inst_mst
--    ADD CONSTRAINT wf_inst_mst_pkey PRIMARY KEY (id);

-- ALTER TABLE ONLY public.wf_status_log
--    ADD CONSTRAINT wf_status_log_pkey PRIMARY KEY (id);

-- ALTER TABLE ONLY public.wf_type_mst
--    ADD CONSTRAINT wf_type_mst_pkey PRIMARY KEY (id);

-- ALTER TABLE ONLY public.leave_wf_inst
--    ADD CONSTRAINT fk2usglhhnfgbd5da0tklxcxv5k FOREIGN KEY (id) REFERENCES public.wf_inst_mst(id);

-- ALTER TABLE ONLY public.loan_wf_inst
--    ADD CONSTRAINT fkloanforrignkeyreference FOREIGN KEY (id) REFERENCES public.wf_inst_mst(id);

DROP TABLE IF EXISTS public.STATE;
DROP TABLE IF EXISTS public.DEFERRED_EVENTS;
DROP TABLE IF EXISTS public.GUARD;
DROP TABLE IF EXISTS public.STATE_EXIT_ACTIONS;
DROP TABLE IF EXISTS public.STATE_ENTRY_ACTIONS;
DROP TABLE IF EXISTS public.TRANSITION_ACTIONS;
DROP TABLE IF EXISTS public.STATE_STATE_ACTIONS;
DROP TABLE IF EXISTS public."ACTION";
DROP TABLE IF EXISTS public.TRANSITION;
DROP TABLE IF EXISTS public.STATE_MACHINE;
