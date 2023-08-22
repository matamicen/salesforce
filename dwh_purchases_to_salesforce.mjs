import pkg from 'pg';
import axios from 'axios';

export const handler = async (event, context) => {
  const { Client } = pkg;
  
  var aux_productInterested = '';
  let aux_company_size;
  let comp = '';
  
  const client = new Client({
    host: 'datawarehouse.cwplwkypor1m.eu-west-3.rds.amazonaws.com',
    port: 5432,
    database: 'dwh',
    user: 'm.micenmacher',
    password: 'N48kNiJ7u9rmD7e.TNKk'
  });

  try {
    // Connect to the PostgreSQL database
    console.log('antes de conectar');


        // Make the API POST call to get Login token in Production
        const response = await axios.post('https://climatetrade12345.my.salesforce.com/services/oauth2/token', 'grant_type=password&client_id=3MVG9SOw8KERNN09U8g_VflqPuiFXOOx.ZyWadR5bnMgiqx12Rlw2brH21F.18CNc400h5KLIIADMmH5oCB4P&client_secret=49A4B605743221AF88DB6BFFE3E3CCC60D385C1D9654EACF7009A80885511DFB&username=matias@climatetrade.com&password=ClimateApi2023', {
            headers: {
              'Content-Type': 'application/x-www-form-urlencoded',
              'Accept': '*/*'
            }
          });
        
        
        // login sandbox
        // const response = await axios.post('https://climatetrade12345--sanboxct.sandbox.my.salesforce.com/services/oauth2/token', 'grant_type=password&client_id=3MVG9SOw8KERNN09U8g_VflqPuiFXOOx.ZyWadR5bnMgiqx12Rlw2brH21F.18CNc400h5KLIIADMmH5oCB4P&client_secret=49A4B605743221AF88DB6BFFE3E3CCC60D385C1D9654EACF7009A80885511DFB&username=leticia.fernandez@climatetrade.com.sanboxct&password=Salesforce..2024LA9kmkqebts7IT6re5LbqSh13', {
        //     headers: {
        //       'Content-Type': 'application/x-www-form-urlencoded',
        //       'Accept': '*/*'
        //     }
        //   });
      
          
        // Make the API POST call
    const accessToken = response.data.access_token;
    //const accessToken = 'asas';
    console.log('Token: '+accessToken);

    await client.connect();
    console.log('Conecto');
    
    //     return {
    //   statusCode: 300,
    //   body: 'Lo frene yo a proposito.'
    // };

    // Execute the stored procedure to select the new signups and insert in an aux_table to process in the next step
    // await client.query('CALL salesforce.select_users()');
    await client.query('CALL salesforce.select_purchases()');
    

    
    //         return {
    //   statusCode: 300,
    //   body: 'Lo frene yo a proposito.'
    // };
    
    
    
    // Execute the select query on aux_table
    // const query = 'SELECT * FROM salesforce.tmp_signups';
    const query = 'SELECT * FROM salesforce.tmp_purchases';
    const result = await client.query(query);
    
    // Keep the first row and remove the rest
    // const firstRow = result.rows[0];
    // result.rows = [firstRow];
     console.log('Purchases a dar de alta en salesforce');
     console.log(result.rows);
     
    // return {
    //   statusCode: 300,
    //   body: 'Lo frene yo a proposito.'
    // };

    // Print each row to the console and call the stored procedure with parameters
    for (const row of result.rows) {
      console.log('project:', row.purchase_id);
      console.log('date_created:', row.purchase_datetime);
      console.log('---');
      
     // const description = row.project_description.substring(0, 2000);
     
     if (row.user_type == 'Business')
        comp = 'Company'
      else
        comp = 'Individual'
        
        console.log('comp: '+comp);

    
    // const leadApiPostResponse = await axios.post('https://climatetrade12345--sanboxct.sandbox.my.salesforce.com/services/apexrest/orderapipost', {
       const leadApiPostResponse = await axios.post('https://climatetrade12345.my.salesforce.com/services/apexrest/orderapipost', {
          "purchase_id": row.purchase_id,
          "user_id": row.user_id,
          "email": row.email,
          "first_name": row.first_name,
          "last_name": row.last_name,
          "country_iso2": row.country_iso2,
          "phone": row.phone,
          "user_type": comp,
          "company": row.company,
          "project_id": row.project_id,
          "project": row.project,
          "purchase_type": row.purchase_type,
          "unit": row.unit,
          "amount": row.amount,
          "amount_eur": row.amount_eur,
          "amount_usd": row.amount_usd,
          "co2_amount": row.co2_amount,
          "purchase_datetime": row.purchase_datetime

    }, {
      headers: {
        'Authorization': `Bearer ${accessToken}`,
        'Content-Type': 'application/json',
        'Accept': '*/*'
      }
    });
    



    const leadApiResponseJson = JSON.parse(leadApiPostResponse.data);
    console.log( leadApiResponseJson);
    //console.log( leadApiResponseJson.action);
    
    
    // return {
    //   statusCode: 300,
    //   body: 'Lo frene yo a proposito.'
    // };
    
       console.log("Matias llamo a la API salesforce");
        if ( leadApiResponseJson.action === 'created' ){
          console.log("new purchase was created in salesforce ");
          //console.log(leadApiResponseJson);
          // Call the stored procedure with parameters to log the processeced customer
          const insertQuery = 'CALL salesforce.insert_processed_purchase($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16, $17, $18, $19)';
        
          
          const values = [
          row.purchase_id,
          row.user_id,
          row.email,
          row.first_name,
          row.last_name,
          row.country_iso2,
          row.phone,
          row.user_type,
          row.company,
          row.project_id,
          row.project,
          row.purchase_type,
          row.unit,
          row.amount,
          row.amount_eur,
          row.amount_usd,
          row.co2_amount,
          row.purchase_datetime,            
          leadApiResponseJson.message
          ];
          await client.query(insertQuery, values);
        }else
        {
          // prefiero por ahora que si falla, frene todo el proceso
          // try {
          console.log("the new signup migration to salesforce fails");
          console.log("length of error: "+ leadApiResponseJson.message.length);
          // console.log(leadApiResponseJson);
          const insertQuery = 'CALL salesforce.insert_error_purchase($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16, $17, $18, $19)';
          const values = [
          row.purchase_id,
          row.user_id,
          row.email,
          row.first_name,
          row.last_name,
          row.country_iso2,
          row.phone,
          row.user_type,
          row.company,
          row.project_id,
          row.project,
          row.purchase_type,
          row.unit,
          row.amount,
          row.amount_eur,
          row.amount_usd,
          row.co2_amount,
          row.purchase_datetime,            
          leadApiResponseJson.message
          ];
          await client.query(insertQuery, values);
          
          // } catch  (err) {
          //   console.log('error en llamar al salesforce.insert_error_signup:');
          //   console.log(err);
          // }
          
          
        }
    
    }

   

    return {
      statusCode: 200,
      body: 'Stored procedure executed successfully.'
    };
  } catch (err) {
    return {
      statusCode: 500,
      body: err.message
    };
  } finally {
    // Close the database connection
    // await client.end();
    // Close the database connection after a delay
      setTimeout(async () => {
        await client.end();
      }, 5000); // 5 seconds delay
      }
};