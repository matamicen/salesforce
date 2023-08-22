CREATE OR REPLACE PROCEDURE salesforce.insert_processed_projects(p_project_id integer, p_project character varying, p_project_description text, p_unit character varying, p_country_iso2 character varying, p_country character varying, p_project_kind character varying, p_project_type character varying, p_standard_type character varying, p_category character varying, p_date_created timestamp without time zone, p_api_message character varying)
 LANGUAGE plpgsql
AS $procedure$
BEGIN
  -- Increment the date_created parameter by 1 millisecond
  p_date_created := p_date_created + INTERVAL '1 millisecond';
 
  INSERT INTO salesforce.processed_projects (
    project_id,
    project,
    project_description,
    unit,
    country_iso2,
    country,
    project_kind,
    project_type,
    standard_type,
    category,
    date_created,
    api_message,
    date_executed
  )
  VALUES (
    p_project_id,
    p_project,
    p_project_description,
    p_unit,
    p_country_iso2,
    p_country,
    p_project_kind,
    p_project_type,
    p_standard_type,
    p_category,
    p_date_created,
    p_api_message,
    CURRENT_TIMESTAMP
  );
END;
$procedure$
;
