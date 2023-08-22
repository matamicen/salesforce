-- Active: 1669124997622@@datawarehouse.cwplwkypor1m.eu-west-3.rds.amazonaws.com@5432@dwh@salesforce
CREATE OR REPLACE PROCEDURE salesforce.insert_error_purchase(
    p_purchase_id INTEGER,
    p_user_id INTEGER,
    p_email VARCHAR,
    p_first_name TEXT,
    p_last_name TEXT,
    p_user_type TEXT,
    p_company VARCHAR,
    p_project_id INTEGER,
    p_project VARCHAR(150),
    p_purchase_type VARCHAR(200),
    p_unit VARCHAR(100),
    p_amount NUMERIC(14, 2),
    p_amount_eur NUMERIC(14, 2),
    p_amount_usd NUMERIC(14, 2),
    p_co2_amount NUMERIC(14, 3),
    p_purchase_datetime TIMESTAMP,
    p_error VARCHAR(999)
) 
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
