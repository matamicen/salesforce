import pkg from 'pg';
import axios from 'axios';

export const handler = async (event, context) => {
  const { Client } = pkg;
  
  var aux_productInterested = '';
  let aux_company_size;
  
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
        // const response = await axios.post('https://climatetrade12345--sanboxct.sandbox.my.salesforce.com/services/oauth2/token', 'grant_type=password&client_id=3MVG9SOw8KERNN09U8g_VflqPuiFXOOx.ZyWadR5bnMgiqx12Rlw2brH21F.18CNc400h5KLIIADMmH5oCB4P&client_secret=49A4B605743221AF88DB6BFFE3E3CCC60D385C1D9654EACF7009A80885511DFB&username=leticia.fernandez@climatetrade.com.sanboxct&password=Salesforce..2023LA9kmkqebts7IT6re5LbqSh13', {
        //     headers: {
        //       'Content-Type': 'application/x-www-form-urlencoded',
        //       'Accept': '*/*'
        //     }
        //   });
      
          
        // Make the API POST call
    const accessToken = response.data.access_token;
    //const accessToken = 'asas';
    console.log('Access Token: '+accessToken);


    await client.connect();
    console.log('Conecto');

    // Execute the stored procedure to select the new signups and insert in an aux_table to process in the next step
    // await client.query('CALL salesforce.select_users()');
    await client.query('CALL salesforce.select_projects()');
    
    
    
    // Execute the select query on aux_table
    // const query = 'SELECT * FROM salesforce.tmp_signups';
    const query = 'SELECT * FROM salesforce.tmp_projects';
    const result = await client.query(query);
    
    // Keep the first row and remove the rest
    // const firstRow = result.rows[0];
    // result.rows = [firstRow];
     console.log('Projects a dar de alta en salesforce');
     console.log(result.rows);
     
    // return {
    //   statusCode: 300,
    //   body: 'Lo frene yo a proposito.'
    // };

    // Print each row to the console and call the stored procedure with parameters
    for (const row of result.rows) {
      console.log('project:', row.project);
      console.log('date_created:', row.date_created);
      console.log('---');
      
      const description = row.project_description.substring(0, 2000);

    
    // const leadApiPostResponse = await axios.post('https://climatetrade12345--sanboxct.sandbox.my.salesforce.com/services/apexrest/productApiPost', {
      const leadApiPostResponse = await axios.post('https://climatetrade12345.my.salesforce.com/services/apexrest/productApiPost', {
      "project_id": row.project_id,
      "project": row.project,      
      "project_description": description,
      "unit": row.unit,
      "country_iso2": row.country_iso2,
      "country": row.country,
      "project_kind": row.project_kind,
      "project_type": row.project_type,
      "standard_type": row.standard_type,
      "category": row.category
    }, {
      headers: {
        'Authorization': `Bearer ${accessToken}`,
        'Content-Type': 'application/json',
        'Accept': '*/*'
      }
    });
    
    //     project_id: 562,
    // project: 'EVERGREEN REDD+ PROJECT',
    // project_description: 'Empty Description',
    // unit: 'Ton. CO2',
    // country_iso2: 'BR',
    // country: 'Brazil',
    // project_kind: 'compensation',
    // project_type: null,
    // standard_type: null,
    // category: null,
    // date_created: 2023-06-16T11:50:44.594Z


    const leadApiResponseJson = JSON.parse(leadApiPostResponse.data);
    console.log( leadApiResponseJson);
    //console.log( leadApiResponseJson.action);
    
    
    // return {
    //   statusCode: 300,
    //   body: 'Lo frene yo a proposito.'
    // };
    
    console.log("Matias llamo a la API salesforce");
        if ( leadApiResponseJson.action === 'created' || leadApiResponseJson.action === 'updated'){
          console.log("new signup was created in salesforce ");
          //console.log(leadApiResponseJson);
          // Call the stored procedure with parameters to log the processeced customer
          const insertQuery = 'CALL salesforce.insert_processed_projects($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)';
          
          const values = [
            row.project_id,
            row.project,
            description,
            row.unit,
            row.country_iso2,
            row.country,
            row.project_kind,
            row.project_type,
            row.standard_type,
            row.category,
            row.date_created,
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
          const insertQuery = 'CALL salesforce.insert_error_project($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)';
          const values = [
            row.project_id,
            row.project,
            description,
            row.unit,
            row.country_iso2,
            row.country,
            row.project_kind,
            row.project_type,
            row.standard_type,
            row.category,
            leadApiResponseJson.message,
            row.date_created
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