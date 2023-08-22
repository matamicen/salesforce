CREATE OR REPLACE PROCEDURE salesforce.insert_error_purchase(p_purchase_id integer, p_user_id integer, p_email character varying, p_first_name text, p_last_name text, p_country_iso2 character varying, p_phone character varying, p_user_type text, p_company character varying, p_project_id integer, p_project character varying, p_purchase_type character varying, p_unit character varying, p_amount numeric, p_amount_eur numeric, p_amount_usd numeric, p_co2_amount numeric, p_purchase_datetime timestamp without time zone, p_error character varying)
 LANGUAGE plpgsql
AS $procedure$
BEGIN
  -- Increment the purchase_datetime parameter by 1 millisecond
  p_purchase_datetime := p_purchase_datetime + INTERVAL '1 millisecond';


    INSERT INTO salesforce.error_purchases (
        purchase_id,
        user_id,
        email,
        first_name,
        last_name,
        country_iso2,
        phone,
        user_type,
        company,
        project_id,
        project,
        purchase_type,
        unit,
        amount,
        amount_eur,
        amount_usd,
        co2_amount,
        purchase_datetime,
        error_message,
        date_executed
    )
    VALUES (
        p_purchase_id,
        p_user_id,
        p_email,
        p_first_name,
        p_last_name,
        p_country_iso2,
        p_phone,
        p_user_type,
        p_company,
        p_project_id,
        p_project,
        p_purchase_type,
        p_unit,
        p_amount,
        p_amount_eur,
        p_amount_usd,
        p_co2_amount,
        p_purchase_datetime,
        p_error,
        CURRENT_TIMESTAMP
    );


END;
$procedure$
;
