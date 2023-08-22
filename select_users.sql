CREATE OR REPLACE PROCEDURE salesforce.select_users()
 LANGUAGE plpgsql
AS $procedure$DECLARE
  last_date_joined TIMESTAMP;
  last_date_executed TIMESTAMP;
  result_row RECORD;
BEGIN
  -- Retrieve the last executed row and last_date_joined from signups_deltas
  SELECT ps.date_executed, ps.date_joined
  INTO last_date_executed, last_date_joined
  FROM salesforce.processed_signups ps
  ORDER BY ps.date_executed DESC
  LIMIT 1;
  
  -- Drop new_table if it already exists
  EXECUTE 'DROP TABLE IF EXISTS salesforce.tmp_signups';


  -- Create a aux_table to store the selected rows
  --CREATE TABLE salesforce.aux_table (
  --  email VARCHAR,
  --  date_joined TIMESTAMP
  --);
  CREATE TABLE salesforce.tmp_signups (
  id SERIAL PRIMARY KEY,
  first_name TEXT,
  last_name TEXT,
  country_iso2 CHARACTER VARYING(2),
  user_type TEXT,
  company CHARACTER VARYING,
  phone CHARACTER VARYING(255),
  company_size CHARACTER VARYING(255),
  is_producer BOOLEAN,
  email VARCHAR(255),
  date_joined TIMESTAMP
);

  -- Insert data from dwh.dim_users using the last_date_joined as the filter
  INSERT INTO salesforce.tmp_signups (first_name, last_name, country_iso2, user_type, company,phone,company_size,is_producer, email, date_joined)
  SELECT first_name, last_name, country_iso2, user_type, company,phone,company_size,is_producer, email, date_joined
  FROM dwh.dim_users
  WHERE date_joined > last_date_joined
  ORDER BY date_joined ASC;
END;
$procedure$
;
