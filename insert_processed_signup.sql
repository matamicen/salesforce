CREATE OR REPLACE PROCEDURE salesforce.insert_processed_signup(p_first_name text, p_last_name text, p_country_iso2 character varying, p_user_type text, p_company character varying, p_phone character varying, p_company_size character varying, p_email character varying, p_is_producer boolean, p_api_message character varying, p_date_joined timestamp without time zone)
 LANGUAGE plpgsql
AS $procedure$
BEGIN
  -- Increment the date_joined parameter by 1 millisecond
  p_date_joined := p_date_joined + INTERVAL '1 millisecond';
 
  INSERT INTO salesforce.processed_signups (
    first_name,
    last_name,
    country_iso2,
    user_type,
    company,
    phone,
    company_size,
    email,
    is_producer,
    date_joined,
    api_message,
    date_executed
  )
  VALUES (
    p_first_name,
    p_last_name,
    p_country_iso2,
    p_user_type,
    p_company,
    p_phone,
    p_company_size,
    p_email,
    p_is_producer,
    p_date_joined,
    p_api_message,
    CURRENT_TIMESTAMP
  );
END;
$procedure$
;
