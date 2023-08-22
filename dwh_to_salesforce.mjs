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


        // Make the API POST call to get token
        const response = await axios.post('https://climatetrade12345.my.salesforce.com/services/oauth2/token', 'grant_type=password&client_id=3MVG9SOw8KERNN09U8g_VflqPuiFXOOx.ZyWadR5bnMgiqx12Rlw2brH21F.18CNc400h5KLIIADMmH5oCB4P&client_secret=49A4B605743221AF88DB6BFFE3E3CCC60D385C1D9654EACF7009A80885511DFB&username=matias@climatetrade.com&password=ClimateApi2023', {
            headers: {
              'Content-Type': 'application/x-www-form-urlencoded',
              'Accept': '*/*'
            }
          });
      
          
        // Make the API POST call
    const accessToken = response.data.access_token;


    await client.connect();
    console.log('Conecto');

    // Execute the stored procedure to select the new signups and insert in an aux_table to process in the next step
    await client.query('CALL salesforce.select_users()');
    
    // Execute the select query on aux_table
    const query = 'SELECT * FROM salesforce.tmp_signups';
    const result = await client.query(query);
    
    // Keep the first row and remove the rest
    // const firstRow = result.rows[0];
    // result.rows = [firstRow];
     console.log('Signups a dar de alta en salesforce');
     console.log(result.rows);

    // Print each row to the console and call the stored procedure with parameters
    for (const row of result.rows) {
      console.log('email:', row.email);
      console.log('date_joined:', row.date_joined);
      console.log('---');
      
      
     
      if (row.is_producer)
       // this makes salesforce create the lead under Supply insted of Sales
        aux_productInterested = 'Selling credits/projects';
      else
        aux_productInterested = '';
        
      if (row.user_type==='Individual')
        aux_company_size = 'Person'
      else
      {
        
        switch (row.company_size) {
          case 'small':
            aux_company_size = 'Small';
            break;
          case 'medium':
            aux_company_size = 'Medium';
            break;
          case 'large':
            aux_company_size = 'Large';
            break;
          default:
            // Default value in case row.company_size doesn't match any of the cases
            aux_company_size = 'Small';
            break;
        }
      }
      
          // Make the second API POST call
    const leadApiPostResponse = await axios.post('https://climatetrade12345.my.salesforce.com/services/apexrest/leadApiPost', {
      "firstName": row.first_name,
      "lastName": row.last_name,
      "email": row.email,
      "message": "",
      "productInterested": aux_productInterested,
      "phone": row.phone,
      "company": row.company,
      "leadSource": "Sign up Form",
      "customerType": aux_company_size,
      "countryCode": row.country_iso2,
      "recordTypeID": "0129W0000004IR2",
      "ownerId": "0059W000000JFD0QAO",
      "city": ""
    }, {
      headers: {
        'Authorization': `Bearer ${accessToken}`,
        'Content-Type': 'application/json',
        'Accept': '*/*'
      }
    });


    const leadApiResponseJson = JSON.parse(leadApiPostResponse.data);
    console.log( leadApiResponseJson.action);
    
    console.log("Matias llamo a la API salesforce");
        if ( leadApiResponseJson.action === 'created'){
          console.log("new signup was created in salesforce ");
          console.log(leadApiResponseJson);
          // Call the stored procedure with parameters to log the processeced customer
          const insertQuery = 'CALL salesforce.insert_processed_signup($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11)';
          const values = [
            row.first_name,
            row.last_name,
            row.country_iso2,
            row.user_type,
            row.company,
            row.phone,
            row.company_size,
            row.email,
            row.is_producer,
            leadApiResponseJson.message,
            row.date_joined,
          ];
          await client.query(insertQuery, values);
        }else
        {
          // prefiero por ahora que si falla, frene todo el proceso
          // try {
          console.log("the new signup migration to salesforce fails");
          console.log("length of error: "+ leadApiResponseJson.message.length);
          console.log(leadApiResponseJson);
          const insertQuery = 'CALL salesforce.insert_error_signup($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11)';
          const values = [
            row.first_name,
            row.last_name,
            row.country_iso2,
            row.user_type,
            row.company,
            row.phone,
            row.company_size,
            row.email,
            row.is_producer,
            leadApiResponseJson.message,
            row.date_joined
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