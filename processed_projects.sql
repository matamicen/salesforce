CREATE TABLE salesforce.processed_projects (
	id serial4 NOT NULL,
	project_id int4 NULL,
	project varchar(150) NULL,
	project_description text NULL,
	unit varchar(100) NULL,
	country_iso2 varchar(2) NULL,
	country varchar(100) NULL,
	project_kind varchar NULL,
	project_type varchar NULL,
	standard_type varchar NULL,
	category varchar NULL,
	date_created timestamp NULL,
	api_message varchar(999) NULL,
	date_executed timestamp NULL,
	CONSTRAINT processed_projects_pkey PRIMARY KEY (id)
);