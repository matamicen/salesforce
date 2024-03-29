CREATE TABLE salesforce.processed_purchases (
	id serial4 NOT NULL,
	purchase_id int4 NULL,
	user_id int4 NULL,
	email varchar NULL,
	first_name text NULL,
	last_name text NULL,
	country_iso2 varchar(2) NULL,
	phone varchar(255) NULL,
	user_type text NULL,
	company varchar NULL,
	project_id int4 NULL,
	project varchar(150) NULL,
	purchase_type varchar(200) NULL,
	unit varchar(100) NULL,
	amount numeric(14, 2) NULL,
	amount_eur numeric(14, 2) NULL,
	amount_usd numeric(14, 2) NULL,
	co2_amount numeric(14, 3) NULL,
	purchase_datetime timestamp NULL,
	api_message varchar(999) NULL,
	date_executed timestamp NULL,
	CONSTRAINT processed_purchases_pkey PRIMARY KEY (id)
);