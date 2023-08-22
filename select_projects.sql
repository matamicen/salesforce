CREATE OR REPLACE PROCEDURE salesforce.select_projects()
 LANGUAGE plpgsql
AS $procedure$DECLARE
  last_date_created TIMESTAMP;
  last_date_executed TIMESTAMP;
  result_row RECORD;
BEGIN
  -- Retrieve the last executed row and last_date_created from processed_projects
  SELECT pp.date_executed, pp.date_created
  INTO last_date_executed, last_date_created
  FROM salesforce.processed_projects pp
  ORDER BY pp.date_executed DESC
  LIMIT 1;
  
  -- Drop new_table if it already exists
  EXECUTE 'DROP TABLE IF EXISTS salesforce.tmp_projects';


  -- Create a aux_table to store the selected rows
  --CREATE TABLE salesforce.aux_table (
  --  email VARCHAR,
  --  date_joined TIMESTAMP
  --);
--   CREATE TABLE salesforce.tmp_signups (
--   id SERIAL PRIMARY KEY,
--   first_name TEXT,
--   last_name TEXT,
--   country_iso2 CHARACTER VARYING(2),
--   user_type TEXT,
--   company CHARACTER VARYING,
--   phone CHARACTER VARYING(255),
--   company_size CHARACTER VARYING(255),
--   is_producer BOOLEAN,
--   email VARCHAR(255),
--   date_joined TIMESTAMP
-- );

  CREATE TABLE salesforce.tmp_projects (
    id SERIAL PRIMARY KEY,
    project_id integer,
    project VARCHAR(150),
    project_description TEXT,
    unit VARCHAR(100),
    country_iso2 VARCHAR(2),
    country VARCHAR(100),
    project_kind VARCHAR,
    project_type VARCHAR,
    standard_type VARCHAR,
    category VARCHAR,
    date_created TIMESTAMP
    );

  -- Insert data from dwh.dim_projects using the last_date_created as the filter
  INSERT INTO salesforce.tmp_projects (project_id, project, project_description, unit, country_iso2, country, project_kind, project_type, standard_type, category, date_created)
  SELECT project_id, project, project_description, unit, country_iso2, country, project_kind, project_type, standard_type, category, created
  FROM dwh.dim_projects
  WHERE created > last_date_created
  ORDER BY created ASC;
END;
$procedure$
;
