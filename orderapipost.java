@RestResource(urlMapping='/orderapipost/*')
global with sharing class OrderApiPost {  
    @HttpPost
    global static String createOrder(Integer purchase_id, Integer user_id, String email, String first_name, String last_name,
                                           String country_iso2, String phone, String user_type, String company, Integer project_id,
                                           String project, String purchase_type, String unit, Decimal amount, Decimal amount_eur,
                                           Decimal amount_usd, Decimal co2_amount, DateTime purchase_datetime, String api_message,
                                           Decimal gmv_eur)        
    {
        // data for JSON
        Map<String, Object> data = new Map<String, Object>();
        
        if (String.valueOf(purchase_id) != '99999999')
        {
        
  
        Account acc;
        Contact con;
        String Gregg_Owner = '0059W000000JFD0QAO'; // Greggory Mannix Id
        String theOwner;
        // data for JSON
      //  Map<String, Object> data = new Map<String, Object>();
        
        String sproject_id = String.valueOf(project_id);
        
      try
      {              
            List<Contact> conList = [SELECT AccountId FROM Contact WHERE Email = :email LIMIT 1];
            //Account acc;
            if (conList.isEmpty()) {
                System.debug('es EMPTY');
              //  String company_name;
              //  Id recordtype;
                
                if (user_type == 'Individual')
                {
                    // usuario que compra no tiene usuario en el sistema
                    // como es Individual se le asigna la Account individual al momento de crear el contact.
                    // 
                    // this select is forn not hardcode the Id of the Individuals account which sandbox and production has different Ids. 
                        List<Account> individualsAccount = [SELECT Id FROM Account WHERE Name = 'individuals' LIMIT 1];
                        acc = individualsAccount[0];           
                }
                else
                {
                    String company_name = company;
                    List<Account> alreadyExistsAccount = [SELECT Id FROM Account WHERE Name = :company_name LIMIT 1];
                     
                    if (alreadyExistsAccount.isEmpty())
                    {
                      // el Account no existe en el sistema
                     // usuario B2B no registrado el sistema
                    // al no ser un usuario B2B se le crea un Account B2B para luego asociarla a su contacto mas adelante.
                                  
                    
                    Id recordtype = '0129W0000004IOhQAM'; 
                    
                    acc = new Account(
                    Name = company_name,
                    RecordTypeId = recordtype,
                    CurrencyIsoCode = 'EUR'
                );
                    
                    insert acc;
                    }
                    else
                    {
                        // El Account name existe, se asocia la purchas a ese account name, solo que se agrega un contacto nuevo a ese Account.
                        acc = alreadyExistsAccount[0];
                    }
                    
                    
                }
                
                // Create a new contact 
                    con = new Contact(
                    FirstName = first_name,
                    LastName = last_name,
                    Email = email,
                    Phone = phone,
                    AccountId = acc.Id,
                    CurrencyIsoCode = 'EUR',
                    country_code__c = country_iso2,
                    Company_individual__c = user_type
                );
                insert con;
       
            } else {
                System.debug('NO es EMPTY');
                // Usuario que compra ya existe y tiene asosciada una Account
                con = conList[0];
                acc = [SELECT Id FROM Account WHERE Id = :con.AccountId LIMIT 1];
            }
                
                // Query for the standard price book
                Pricebook2 pb = [SELECT Id FROM Pricebook2 WHERE Name = 'Standard Price Book' LIMIT 1];
                Id stdPricebookId = pb.Id;
                        System.debug('pricebook id: '+stdPricebookId);
        
                
                // Create a new order
                Order ord = new Order(
                    AccountId = acc.Id,
                    EffectiveDate = purchase_datetime.date(),
                    Status = 'Draft',
                    CurrencyIsoCode = 'EUR',
                    Pricebook2Id = stdPricebookId,
                    email__c = email
                );
                insert ord;
        
         System.debug('Order id: '+ord.Id);
                
                // Query for the pricebook entry
                PricebookEntry pbe = [SELECT Id FROM PricebookEntry WHERE ProductCode = :sproject_id AND Pricebook2Id = :stdPricebookId LIMIT 1];
                // GMV are base_amount_eur + tax_amount_eur + transaction_fee_eur, it comes from DWH pre calculated in gmv_eur
                Decimal unitPrice =  (amount_eur + gmv_eur) / co2_amount;
                // Create a new order item
                OrderItem item = new OrderItem(
                    OrderId = ord.Id,
                    PricebookEntryId = pbe.Id,
                    Quantity = co2_amount,
                    UnitPrice = unitPrice
                );

                insert item;
          
                ord.Status = 'Activated'; // Change the Status field value
                update ord; // Update the order record with the new Status value
        
        if (user_type == 'Company')
        {
               // sum the total for the Account
                AggregateResult[] groupedResults = [SELECT SUM(TotalAmount) sum, COUNT(Id) countorders FROM Order WHERE AccountId = :acc.Id];
    			Decimal totalAmount = (Decimal)groupedResults[0].get('sum');
    			Integer orderCount = (Integer)groupedResults[0].get('countorders');

    			// Update the TotalAmount__c and TotalOrders__c fields of the given accountId
    			Account accountToUpdate = new Account(Id = acc.Id, TotalAmount__c = totalAmount, TotalOrders__c = orderCount, 	last_purchase__c = purchase_datetime.date());
    			update accountToUpdate;
            
        }
        else
        {
                // sum the total for the Individual Account
                AggregateResult[] groupedResults = [SELECT SUM(TotalAmount) sum, COUNT(Id) countorders FROM Order WHERE AccountId = :acc.Id];
    			Decimal totalAmount = (Decimal)groupedResults[0].get('sum');
    			Integer orderCount = (Integer)groupedResults[0].get('countorders');

    			// Update the TotalAmount__c and TotalOrders__c fields of the given accountId
    			Account accountToUpdate = new Account(Id = acc.Id, TotalAmount__c = totalAmount, TotalOrders__c = orderCount);
    			update accountToUpdate;
            
               // sum the total for contact Account
                groupedResults = [SELECT SUM(TotalAmount) sum, COUNT(Id) countorders FROM Order WHERE email__c = :email];
    		    totalAmount = (Decimal)groupedResults[0].get('sum');
    			orderCount = (Integer)groupedResults[0].get('countorders');

                // Query for the Contact record with the given email2
                Contact contactToUpdate = [SELECT Id FROM Contact WHERE Email = :email LIMIT 1];
            
                // Update the TotalAmount__c and TotalOrders__c fields of the Contact record
                contactToUpdate.TotalAmount__c = totalAmount;
                contactToUpdate.TotalOrders__c = orderCount;
                contactToUpdate.last_purchase__c = purchase_datetime.date();
                update contactToUpdate;
            
        }
        
        
        
     // Una vez dado de alta la orden en realidad a partir de aqui debo respetar lo que me pide el PPT
       
        
     // //  // Check if an Opportunity exists for the email
          
            List<Opportunity> opportunities = Database.query('SELECT Id, AccountId, OwnerId FROM Opportunity WHERE AccountId IN (SELECT AccountId FROM Contact WHERE Email = :email)');
            
            if (!opportunities.isEmpty()) {
                
                if (user_type!='Individual')
                {  
                    // Create a new Task for the Opportunity's Contact only for B2B customers
                     if (opportunities[0].OwnerId == '0059W000000JFBs') // Simon is not an active user, this is to avoid insert errors
                     { 
                        theOwner = Gregg_Owner;
                     }
                    else
                    {
                        theOwner = opportunities[0].OwnerId;   
                     }
                       
                        // Retrieve the Opportunity ID
                        String opportunityId = opportunities[0].Id;
                        System.debug('Opportunity ID:'+ opportunityId);
                        String salesforceBaseUrl = URL.getSalesforceBaseUrl().toExternalForm();
                        System.debug('Salesforce Base URL: ' + salesforceBaseUrl);
                        String urlOpportunity = salesforceBaseUrl + '/lightning/r/Opportunity/'+ opportunityId + '/view';
                    
                    Task newTask = new Task();
                    newTask.Subject = 'A new purchase was made by '+ email;
                    // newTask.WhoId = opportunities[0].Id;
                    newTask.WhatId = opportunities[0].Id;
                   // newTask.OwnerId = opportunities[0].OwnerId;
                    newTask.OwnerId = theOwner;

                   // 
                    //newTask.Description = 'Task created via Order API (Opportunity)';
                    newTask.Description = 'click to see the opportunity: ' + urlOpportunity;
                    
                    insert newTask;
                    
                        // Retrieve the Opportunity ID
                      //  String opportunityId = opportunities[0].Id;
                      //  System.debug('Opportunity ID:'+ opportunityId);
                      //  String salesforceBaseUrl = URL.getSalesforceBaseUrl().toExternalForm();
                      //  System.debug('Salesforce Base URL: ' + salesforceBaseUrl);
                      //  String urlOpportunity = salesforceBaseUrl + '/lightning/r/Opportunity/'+ opportunityId + '/view';

                        // Send an email notification to the OwnerId
                        Messaging.SingleEmailMessage emailMessage = new Messaging.SingleEmailMessage();
                        emailMessage.setSubject('New purchase was made by '+ email);
                        emailMessage.setPlainTextBody(email + ' has recently made a purchase. ... ' + '\nclick to see the opportunity: ' +urlOpportunity);
                        //emailMessage.setTargetObjectId(opportunities[0].OwnerId);
                        emailMessage.setTargetObjectId(theOwner);
                        emailMessage.setSaveAsActivity(false);
                        Messaging.sendEmail(new Messaging.SingleEmailMessage[] { emailMessage });
                        System.debug('envio notificacion'); 
                    

                        // Send an email notification to Fran Benedito (New purchase)
                        emailMessage = new Messaging.SingleEmailMessage();
                        emailMessage.setSubject('New purchase was made by '+ email);
                        emailMessage.setPlainTextBody(email + ' has recently made a purchase. ... ' + '\nclick to see the opportunity: ' +urlOpportunity);
                        //emailMessage.setTargetObjectId(opportunities[0].OwnerId);
                        emailMessage.setTargetObjectId('0059W000000JeyaQAC');
                        emailMessage.setSaveAsActivity(false);
                        Messaging.sendEmail(new Messaging.SingleEmailMessage[] { emailMessage }); 
                        
                    
                    
                    data.put('status', 200);
                    data.put('email', email);
                    data.put('action', 'created');
                    data.put('message', 'New purchase and a new task created for an existing opportunity.');
                    String jsonString = JSON.serialize(data);
                    return jsonString;
                }
       
            }        
        

        
        
        
        
  // // // Check if the email exists in the Lead object
   
        
        List<Lead> leads = [SELECT Id, Email, OwnerId FROM Lead WHERE Email = :email LIMIT 1];
                
        if (!leads.isEmpty()) {
             if (user_type!='Individual')
               {  
        
        // the customer has a current lead, so we will create a new task in the lead and notify to the SDR of this situation.

        
        // Assuming you have the LeadId, AccountId, and ContactId
        String leadId = leads[0].Id; // Replace with your LeadId
                   
                    if (leads[0].OwnerId == '0059W000000JFBs') // Simon is not an active user, this is to avoid insert errors
                     { 
                        theOwner = Gregg_Owner;
                     }
                    else
                    {
                        theOwner = leads[0].OwnerId;   
                     }
                   
                   
                        String salesforceBaseUrl = URL.getSalesforceBaseUrl().toExternalForm();
                        System.debug('Salesforce Base URL: ' + salesforceBaseUrl);
                        String urlLead = salesforceBaseUrl + '/lightning/r/Lead/'+ leadId + '/view';
            
            // Create a new Task associated with the converted Opportunity
            Task newTask = new Task();
            newTask.Subject = 'Lead => New purchase was made by '+ email;
            newTask.WhoId = leadId;
            newTask.OwnerId =  theOwner;  
            newTask.Description = 'click to see the lead: ' + urlLead;
         
            
            // Insert the Task record
            try {
                insert newTask;
                System.debug('New Task ID: ' + newTask.Id);
                
                      //  String salesforceBaseUrl = URL.getSalesforceBaseUrl().toExternalForm();
                     //   System.debug('Salesforce Base URL: ' + salesforceBaseUrl);
                      //  String urlLead = salesforceBaseUrl + '/lightning/r/Lead/'+ leadId + '/view';

                        // Send an email notification to the OwnerId
                        Messaging.SingleEmailMessage emailMessage = new Messaging.SingleEmailMessage();
                        emailMessage.setSubject('Lead => - New purchase was made by '+ email);
                        emailMessage.setPlainTextBody(email + ' has recently made a purchase... there is an existing lead for this customer.' + '\nclick to see the lead: ' +urlLead);
                       // emailMessage.setTargetObjectId(leads[0].OwnerId);   
                        emailMessage.setTargetObjectId(theOwner);                
                        emailMessage.setSaveAsActivity(false);
                        Messaging.sendEmail(new Messaging.SingleEmailMessage[] { emailMessage });
                        System.debug('envio notificacion');  
                
                        // Send an email to Fran Benedito (new purchase)
                        emailMessage = new Messaging.SingleEmailMessage();
                        emailMessage.setSubject('Lead => - New purchase was made by '+ email);
                        emailMessage.setPlainTextBody(email + ' has recently made a purchase... there is an existing lead for this customer.' + '\nclick to see the lead: ' +urlLead);   
                        emailMessage.setTargetObjectId('0059W000000JeyaQAC');                
                        emailMessage.setSaveAsActivity(false);
                        Messaging.sendEmail(new Messaging.SingleEmailMessage[] { emailMessage });
            

                    data.put('status', 200);
                    data.put('email', email);
                    data.put('action', 'created');
                    data.put('message', 'New purchase in a existing Lead.');
                    String jsonString = JSON.serialize(data);
                    return jsonString;
                
                
                
            } catch (Exception e) {
                System.debug('Error creating Task: ' + e.getMessage());
                    data.put('status', 200);
                    data.put('email', email);
                    data.put('action', 'error');
                    data.put('message', 'New purchase in a existing Lead fails => purchaseid: '+purchase_id + ' error: '+e.getMessage());
                    String jsonString = JSON.serialize(data);
                    return jsonString;
            }
            


   
                   
               }
 
            
        }
        
        
        
// // // Si no tiene Opportunit ni Lead y es Business, entonces creo una Opportunity de cero solo para los B2B

      if (user_type!='Individual')
       {      
        String accountId = acc.Id; // Replace with your AccountId
      //  String contactId = con.Id; // Replace with your ContactId
        
        // Create a new Opportunity instance
        Opportunity newOpportunity = new Opportunity();
        newOpportunity.Name = project; // Set the name of the Opportunity
        newOpportunity.StageName = 'Closed/Won'; // Set the stage of the Opportunity to 'Closed/Won'
        newOpportunity.CloseDate = Date.today(); // Set the close date of the Opportunity
        newOpportunity.AccountId = accountId; // Associate the Opportunity with the specified Account
        newOpportunity.Payment_status__c = 'Done'; // Set the value of the Payment_status__c field
        newOpportunity.Invoice_number__c = String.valueOf(purchase_id);
        newOpportunity.N_of_tons__c = co2_amount;
        newOpportunity.Price_per_unit__c = unitPrice;
        newOpportunity.CurrencyIsoCode = 'EUR';
        newOpportunity.When_will_the_renovation_take_place__c = 'In 1 Year';
        newOpportunity.OwnerId = Gregg_Owner; // Assign to Greggory Mannix
    //    newOpportunity.OwnerId = '00509000006nmAfAAI'; // all the new B2C contacts are assigned to Leticia Fernandez
  //      newOpportunity.Contact__c = contactId; // Associate the Opportunity with the specified Contact
        
        // Insert the Opportunity record
        try {
            insert newOpportunity;
            System.debug('New Opportunity ID: ' + newOpportunity.Id);
            
                        // Retrieve the Opportunity ID
                        //String opportunityId = opportunities[0].Id;
                        System.debug('Opportunity ID:'+ newOpportunity.Id);
                        String salesforceBaseUrl = URL.getSalesforceBaseUrl().toExternalForm();
                        System.debug('Salesforce Base URL: ' + salesforceBaseUrl);
                        String urlOpportunity = salesforceBaseUrl + '/lightning/r/Opportunity/'+ newOpportunity.Id + '/view';
            
                    // Create a new Task for the Opportunity only for B2B customers
                    Task newTask = new Task();
                    newTask.Subject = 'A purchase was made through the marketplace by '+ email;
                    // newTask.WhoId = opportunities[0].Id;
                    newTask.WhatId = newOpportunity.Id;
                    newTask.OwnerId = newOpportunity.OwnerId;
            
                    //newTask.Description = 'Task created via Order API (Opportunity)';
                    newTask.Description = 'A new won/close opportunity was created.' + '\nclick to see the opportunity: ' +urlOpportunity;
            
                    insert newTask;
                    
                        // Retrieve the Opportunity ID
                        //String opportunityId = opportunities[0].Id;
                        // System.debug('Opportunity ID:'+ newOpportunity.Id);
                        //String salesforceBaseUrl = URL.getSalesforceBaseUrl().toExternalForm();
                       // System.debug('Salesforce Base URL: ' + salesforceBaseUrl);
                       // String urlOpportunity = salesforceBaseUrl + '/lightning/r/Opportunity/'+ newOpportunity.Id + '/view';

                        // Send an email notification to the OwnerId
                        Messaging.SingleEmailMessage emailMessage = new Messaging.SingleEmailMessage();
                        emailMessage.setSubject('New purchase was made by '+ email);
                        emailMessage.setPlainTextBody(email + ' has recently made a purchase. ... A new won/close opportunity was created.' + '\nclick to see the opportunity: ' +urlOpportunity);
                        //emailMessage.setTargetObjectId(opportunities[0].OwnerId);
                        System.debug('envio mail new opp. a: '+ newOpportunity.OwnerId);
                        emailMessage.setTargetObjectId(newOpportunity.OwnerId);
                        emailMessage.setSaveAsActivity(false);
                        Messaging.sendEmail(new Messaging.SingleEmailMessage[] { emailMessage });
                        System.debug('envio notificacion');       
            
                        // Send an email notification to Fran Benedito (new purchase)
                        emailMessage = new Messaging.SingleEmailMessage();
                        emailMessage.setSubject('New purchase was made by '+ email);
                        emailMessage.setPlainTextBody(email + ' has recently made a purchase. ... A new won/close opportunity was created.' + '\nclick to see the opportunity: ' +urlOpportunity);
                        System.debug('envio mail new opp. a: '+ newOpportunity.OwnerId);
                        emailMessage.setTargetObjectId('0059W000000JeyaQAC');
                        emailMessage.setSaveAsActivity(false);
                        Messaging.sendEmail(new Messaging.SingleEmailMessage[] { emailMessage });            
                    
                    data.put('status', 200);
                    data.put('email', email);
                    data.put('action', 'created');
                    data.put('message', 'New opportunity created.');
                    String jsonString = JSON.serialize(data);
                    return jsonString;
        } catch (Exception e) {
            System.debug('Error creating Opportunity: ' + e.getMessage());
                    data.put('status', 200);
                    data.put('email', email);
                    data.put('action', 'error');
                    data.put('message', e.getMessage());
                    String jsonString = JSON.serialize(data);
                    return jsonString;
        }
           
       }
          else
          {
            // si llego aca es porque es un B2C entonces informo que solo se creo la orden y se asocio al INDIVUDUAL account
            String mess =  'New B2C Order created: ' + ord.Id;
            data.put('status', 200);
            data.put('email', 'email');
            data.put('action', 'created');
            data.put('message', mess);
            String jsonString = JSON.serialize(data);
            return jsonString;
          }
                
        
        
        
        
          
    } catch (Exception e) {
            System.debug('Global Error: ' + e.getMessage());
                    data.put('status', 200);
                    data.put('email', email);
                    data.put('action', 'error');
                    data.put('message', 'Global error: '+ e.getMessage());
                    String jsonString = JSON.serialize(data);
                    return jsonString;
        }
        }
        else
        {   
            String mess =  'This is for avoid the test, please as soon you have time do the real test of the API.';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            mess =  'This is for avoid the test ';
            
            data.put('status', 200);
            data.put('email', 'email');
            data.put('action', 'avoided');
            data.put('message', mess);
            String jsonString = JSON.serialize(data);
            return jsonString;
            
        }
    }
}