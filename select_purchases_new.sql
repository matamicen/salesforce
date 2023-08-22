CREATE OR REPLACE PROCEDURE salesforce.select_purchases()
LANGUAGE plpgsql
AS $procedure$
DECLARE
  last_date_created TIMESTAMP;
  last_date_executed TIMESTAMP;
  result_row RECORD;
BEGIN
  -- Retrieve the last executed row and last_date_created from processed_projects
  SELECT pp.date_executed, pp.purchase_datetime
  INTO last_date_executed, last_date_created
  FROM salesforce.processed_purchases pp
  ORDER BY pp.date_executed DESC
  LIMIT 1;

  -- Drop new_table if it already exists
  EXECUTE 'DROP TABLE IF EXISTS salesforce.tmp_purchases';

  -- Create the temporary table with the new field 'gmv_eur'
  CREATE TABLE salesforce.tmp_purchases (
    id SERIAL PRIMARY KEY,
    purchase_id INTEGER,
    user_id INTEGER,
    email VARCHAR,
    first_name TEXT,
    last_name TEXT,
    country_iso2 CHARACTER VARYING(2),
    phone CHARACTER VARYING(255),
    user_type TEXT,
    company VARCHAR,
    project_id INTEGER,
    project VARCHAR(150),
    purchase_type VARCHAR(200),
    unit VARCHAR(100),
    amount NUMERIC(14, 2),
    amount_eur NUMERIC(14, 2),
    amount_usd NUMERIC(14, 2),
    co2_amount NUMERIC(14, 3),
    purchase_datetime TIMESTAMP,
    gmv_eur NUMERIC(14, 2)
  );

  -- Insert data into salesforce.tmp_purchases and fill gmv_eur field using the join
  INSERT INTO salesforce.tmp_purchases (
    purchase_id, user_id, email, first_name, last_name, country_iso2, phone, user_type, company,
    project_id, project, purchase_type, unit, amount, amount_eur, amount_usd, co2_amount, purchase_datetime, gmv_eur
  )
  SELECT
    fp.purchase_id, fp.user_id, du.email, du.first_name, du.last_name, du.country_iso2, du.phone, du.user_type, du.company,
    dp.project_id, dp.project, pt.purchase_type, dp.unit, fp.amount, fp.amount_eur, fp.amount_usd, fp.co2_amount, fp.purchase_datetime,
    COALESCE(fb.gmv_eur, 0) AS gmv_eur
  FROM dwh.fact_purchases AS fp
  JOIN dwh.dim_users AS du ON fp.user_id = du.user_id
  JOIN dwh.dim_project_details AS dpd ON fp.project_detail_id = dpd.project_detail_id
  JOIN dwh.dim_projects AS dp ON dpd.project_id = dp.project_id
  JOIN dwh.dim_purchase_type AS pt ON fp.purchase_type_id = pt.purchase_type_id
  LEFT JOIN dwh.fact_billing AS fb ON fb.purchase_id = fp.purchase_id AND (fb.record_type = 'FEE_CLIENT' or fb.record_type = 'API_BALANCE_GAIN')
  WHERE (pt.purchase_type = 'api-balance' OR pt.purchase_type = 'marketplace') 
    AND fp.purchase_datetime > last_date_created
  ORDER BY fp.purchase_datetime ASC;
END;
$procedure$;

