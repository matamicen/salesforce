CREATE TABLE salesforce.errors_signups (
	id serial4 NOT NULL,
	first_name text NULL,
	last_name text NULL,
	country_iso2 varchar(2) NULL,
	user_type text NULL,
	company varchar NULL,
	phone varchar(255) NULL,
	company_size varchar(255) NULL,
	email varchar(255) NULL,
	is_producer bool NULL,
	date_joined timestamp NULL,
	error_message varchar(999) NULL,
	date_executed timestamp NULL,
	CONSTRAINT errors_signups_pkey PRIMARY KEY (id)
);