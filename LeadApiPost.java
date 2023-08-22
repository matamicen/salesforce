@RestResource(urlMapping='/leadApiPost/*')
global with sharing class LeadApiPost {
    
    @HttpPost
    global static String createTaskForLead(String firstName, String lastName, String email, String subject,  String phone, String message, String productInterested, String company,
                                           String leadSource, String customerType, String countryCode, String recordTypeID, String ownerId, String landing, String city, Boolean newsletter, String language)        
    {
        // data for JSON
        Map<String, Object> data = new Map<String, Object>();
        try
        {
            // if is true is becasue the customer enters the email to the newsleeter box on the marketplace
            if (newsletter==true)
            {
                        // Create a new Contact B2C
                        System.debug('entro newsletter');
                        List<Contact> contactsInDatabase = [SELECT Id FROM Contact WHERE Email = :email];  
                        
                        if (contactsInDatabase.isEmpty()) {
                            
                            
                            Contact newContact = new Contact();
                            newContact.FirstName = firstName;
                            newContact.LastName = lastName;
                            newContact.Email = email;
                            newContact.Company_individual__c = 'Individual';
                            //newContact.city__c = city;
                            if (countryCode=='')
                            {
                                countryCode = '--None--';
                            }
                            newContact.country_code__c = countryCode;
                            
                            newContact.OwnerId = '00509000006nmAfAAI'; // all the new B2C contacts are assigned to Leticia Fernandez
                            //newContact.Phone = phone;
                            //newContact.message__c = message;
                            newContact.newsletter__c = true;
                            
                            // This code is for mapping ISO2 country codes to Languages
                            if (language=='ES' || language=='es') {
                                newContact.Language__c = 'Spanish';
                            } else {
                                newContact.Language__c = 'English';
                            }  
                                    
                            // set RGPD to YES by default
                            Set<String> selectedValues = new Set<String>();
                            selectedValues.add('Yes');
                            newContact.Authorization_RGPD__c = String.join(new List<String>(selectedValues), ';');

                            
                            // this select is forn not hardcode the Id of the Individuals account which sandbox and production has different Ids. 
                            List<Account> individualsAccount = [SELECT Id FROM Account WHERE Name = 'newsletter' LIMIT 1];
                            newContact.AccountId = individualsAccount[0].Id;
                            //   newContact.AccountId = individualsAccount.Id;
                            // newContact.AccountId = '0019b000007CjlaAAC'; // Assign the newly created Account to the Contact
                            insert newContact;
                            
                            data.put('status', 200);
                            data.put('email', email);
                            data.put('action', 'created');
                            data.put('message', 'New contact added to the newsletter.');
                            String jsonString = JSON.serialize(data);
                            return jsonString;
                            
                            
                            // return 'status: 200, contacto B2C creado';
                        }
                        else
                        {
                            data.put('status', 200);
                            data.put('email', email);
                            data.put('action', 'nothing');
                            data.put('message', 'This email already subscribed to the newsletter.');
                            String jsonString = JSON.serialize(data);
                            return jsonString;
                            
                        }
              }     
            else
            {
            
            
            System.debug('No entro newsletter');
            
            
            
            
            
            
            
            // Check if an Opportunity exists for the email
            //        List<Opportunity> opportunities = [SELECT Id, AccountId, OwnerId FROM Opportunity WHERE AccountId IN (SELECT AccountId FROM Contact WHERE Email = :email)];
            List<Opportunity> opportunities = Database.query('SELECT Id, AccountId, OwnerId, name FROM Opportunity WHERE AccountId IN (SELECT AccountId FROM Contact WHERE Email = :email)');
            
            if (!opportunities.isEmpty()) {
                
                if (customerType!='Person')
                {  
                    // Create a new Task for the Opportunity's Contact only for B2B customers
                    Task newTask = new Task();
                    newTask.Subject = 'Opportunity contact us again!';
                    // newTask.WhoId = opportunities[0].Id;
                    newTask.WhatId = opportunities[0].Id;
                    newTask.OwnerId = opportunities[0].OwnerId;
                    newTask.Description = 'Task created via Custom API (Opportunity)';
                    
                    insert newTask;
                    
                        // Retrieve the Opportunity ID
                        String opportunityId = opportunities[0].Id;
                        System.debug('Opportunity ID:'+ opportunityId);
                        String salesforceBaseUrl = URL.getSalesforceBaseUrl().toExternalForm();
                        System.debug('Salesforce Base URL: ' + salesforceBaseUrl);
                        String urlOpportunity = salesforceBaseUrl + '/lightning/r/Opportunity/'+ opportunityId + '/view';

                        // Send an email notification to the OwnerId
                        Messaging.SingleEmailMessage emailMessage = new Messaging.SingleEmailMessage();
                        emailMessage.setSubject('Your opportunity '+opportunities[0].name + ' has new activity');
                        emailMessage.setPlainTextBody('The person '+ firstName + ' of the company ' + company + ' has (downloaded the paper/registered/has sent an email through the contact form). You have already created an opportunity: ' +urlOpportunity);
                        //emailMessage.setTargetObjectId(opportunities[0].OwnerId);
                        emailMessage.setTargetObjectId(opportunities[0].OwnerId);
                        emailMessage.setSaveAsActivity(false);
                        Messaging.sendEmail(new Messaging.SingleEmailMessage[] { emailMessage });
                        System.debug('envio notificacion');                    
                    
                    
                    data.put('status', 200);
                    data.put('email', email);
                    data.put('action', 'created');
                    data.put('message', 'New task created for an existing opportunity.');
                    String jsonString = JSON.serialize(data);
                    return jsonString;
                }
                else {
                    data.put('status', 200);
                    data.put('email', email);
                    data.put('action', 'nothing');
                    data.put('message', 'B2C customer with an already opportunity');
                    String jsonString = JSON.serialize(data);
                    return jsonString;
                }
            } else {
                
                // Check if the email exists in the Lead object
                List<Lead> leads = [SELECT Id, Email, OwnerId FROM Lead WHERE Email = :email LIMIT 1];
                
                if (!leads.isEmpty()) {
                    if (customerType!='Person')
                    {   
                        // Create a new Task for the Lead
                        Task newTask = new Task();
                        newTask.Subject = 'Lead contact us again!';
                        newTask.WhoId = leads[0].Id;
                        newTask.OwnerId = leads[0].OwnerId; // Assign the Lead's Owner to the Task's Owner
                        newTask.Description = 'Landings: ' + landing + ' on '+ System.now();
                        
                        insert newTask;
                        
                        // update the lastlandings
                        Lead updateLead = leads[0];
                        
                        updateLead.lastlandings__c = landing;
                        updateLead.laslastlandingsdatetlandingsdate__c = System.now();
                        
                        update updateLead;
                        
                        // Retrieve the Lead ID
                        String leadId = leads[0].Id;
                        System.debug('Lead ID:'+ leadId);
                        String salesforceBaseUrl = URL.getSalesforceBaseUrl().toExternalForm();
                        System.debug('Salesforce Base URL: ' + salesforceBaseUrl);
                        String urlLead = salesforceBaseUrl + '/lightning/r/Lead/'+ leadId + '/view';
                        
                        // Send an email notification to the OwnerId
                        Messaging.SingleEmailMessage emailMessage = new Messaging.SingleEmailMessage();
                        emailMessage.setSubject('Your company '+ company + ' has a new activity');
                        emailMessage.setPlainTextBody(firstName + ' of the company '+ company + 'has (downloaded the paper/registered/has sent an email through the contact form). You have already created a lead ' +urlLead);
                        //emailMessage.setTargetObjectId(opportunities[0].OwnerId);
                        emailMessage.setTargetObjectId(leads[0].OwnerId);
                        emailMessage.setSaveAsActivity(false);
                        Messaging.sendEmail(new Messaging.SingleEmailMessage[] { emailMessage });
                        System.debug('envio notificacion');
                        
                        data.put('status', 200);
                        data.put('email', email);
                        data.put('action', 'created');
                        data.put('message', 'New task created for existing lead.');
                        String jsonString = JSON.serialize(data);
                        return jsonString;
                        
                    }
                    else {
                        data.put('status', 200);
                        data.put('email', email);
                        data.put('action', 'nothing');
                        data.put('message', 'B2C customer with an already Lead, nothing to do');
                        String jsonString = JSON.serialize(data);
                        return jsonString;           
                    }
                    
                }
                else {
                    //   return 'Lead not found with email: ' + email;
                    //               // Create a new Lead
                    if (customerType!='Person')
                    {
                        Lead newLead = new Lead();
                        newLead.FirstName = firstName;
                        newLead.LastName = lastName;
                        newLead.Email = email;
                        newLead.Phone = phone;
                        newLead.Message__c = message;
                        newLead.Product_interested_in__c = productInterested;
                        newLead.Company = company;
                        newLead.LeadSource = leadSource;
                        newLead.Customer_type__c = customerType;
                        newLead.country_code__c = countryCode;
                        if (productInterested == 'Selling credits/projects'){
                            // recordtypeid is Supply
                            newLead.RecordTypeID = '0129W0000004Ii3QAE';
                        }
                        else
                        {
                            newLead.RecordTypeID = recordTypeID;
                        }
                       // newLead.RecordTypeID = recordTypeID;
                        newLead.ownerId = ownerId; 
                        newLead.Landings__c = landing;
                        newLead.lastlandings__c = landing;
                        newLead.laslastlandingsdatetlandingsdate__c = System.now();
                        newLead.city__c = city;
                       // if (leadSource=='Marketplace')
                        if ( productInterested == '' )
                        {     
                            if ( leadSource != 'Sign up Form')
                            {
                                newLead.Status = 'Backlog';
                            }
                            else
                            {
                                newLead.Status = 'Pre-Qualification';
                            }
                            
                           // newLead.Tool_interested_in__c = 'Marketplace';  este campo no se usa mas en teoria
                        }
                        else
                        {
                            newLead.Status = 'Pre-Qualification';
                        }
                        
                        insert newLead;
                        
                        // Retrieve the Lead ID
                        String leadId = newLead.Id;
                        System.debug('Lead ID:'+ leadId);
                        String salesforceBaseUrl = URL.getSalesforceBaseUrl().toExternalForm();
                        System.debug('Salesforce Base URL: ' + salesforceBaseUrl);
                        String urlLead = salesforceBaseUrl + '/lightning/r/Lead/'+ leadId + '/view';
                       

                        
                        // Send an email notification to the OwnerId
                        Messaging.SingleEmailMessage emailMessage = new Messaging.SingleEmailMessage();
                        emailMessage.setSubject('You have a new lead created');
                        emailMessage.setPlainTextBody('The person '+firstName+ ' of the company ' + company + ' is a new lead. Here is the link: ' +urlLead );
                        emailMessage.setTargetObjectId(ownerId);
                        emailMessage.setSaveAsActivity(false);
                        Messaging.sendEmail(new Messaging.SingleEmailMessage[] { emailMessage });
                        System.debug('envio notificacion');
                        
                        if ( leadSource == 'Sign up Form')
                        {
                        // Send an email notification to Fran Benedito (New Signup from Marketplace)
                        emailMessage = new Messaging.SingleEmailMessage();
                        emailMessage.setSubject('You have a new lead created');
                        emailMessage.setPlainTextBody('The person '+firstName+ ' of the company ' + company + ' is a new lead. Here is the link: ' +urlLead );
                        emailMessage.setTargetObjectId('00509000006nmAfAAI'); //0059W000000JeyaQAC
                        emailMessage.setSaveAsActivity(false);
                        Messaging.sendEmail(new Messaging.SingleEmailMessage[] { emailMessage });
                        }
                        
                        
                        data.put('status', 200);
                        data.put('email', email);
                        data.put('action', 'created');
                        data.put('message', 'New B2B Lead created.');
                        String jsonString = JSON.serialize(data);
                        return jsonString;           
                        
                    }
                    else
                    {
                        // Create a new Contact B2C
                        
                        List<Contact> contactsInDatabase = [SELECT Id FROM Contact WHERE Email = :email];  
                        
                        if (contactsInDatabase.isEmpty()) {
                            
                            
                            Contact newContact = new Contact();
                            newContact.FirstName = firstName;
                            newContact.LastName = lastName;
                            newContact.Email = email;
                            newContact.Company_individual__c = 'Individual';
                            newContact.city__c = city;
                            newContact.country_code__c = countryCode;
                            newContact.OwnerId = '00509000006nmAfAAI'; // all the new B2C contacts are assigned to Leticia Fernandez
                            newContact.Phone = phone;
                            newContact.message__c = message;
                            
                            // set RGPD to YES by default
                            Set<String> selectedValues = new Set<String>();
                            selectedValues.add('Yes');
                            newContact.Authorization_RGPD__c = String.join(new List<String>(selectedValues), ';');

                            
                            // this select is forn not hardcode the Id of the Individuals account which sandbox and production has different Ids. 
                            List<Account> individualsAccount = [SELECT Id FROM Account WHERE Name = 'individuals' LIMIT 1];
                            newContact.AccountId = individualsAccount[0].Id;
                            //   newContact.AccountId = individualsAccount.Id;
                            // newContact.AccountId = '0019b000007CjlaAAC'; // Assign the newly created Account to the Contact
                            insert newContact;
                            
                            String salesforceBaseUrl = URL.getSalesforceBaseUrl().toExternalForm();
                            String destination = '';
                            if (salesforceBaseUrl.contains('sandbox.my')) {
                                // Sandbox environment
                                 destination = '00509000006nmAfAAI';
                               
                            } else {
                               // destination = '0059W000000JPkYQAW'; // crm@climatetrade.com in production
                                destination = '0059W000000UIHQQA4'; // marketing@climatetrade.com
                            }
                            
                        
                            
                        // Send an email to crm@climatatrade.ocm only if there any message
                        if ( message != '')
                        {
                        String contactId = newContact.Id;
                        salesforceBaseUrl = URL.getSalesforceBaseUrl().toExternalForm();
                        System.debug('Salesforce Base URL: ' + salesforceBaseUrl);
                        System.debug('Destination: '+destination);
                        String urlContact = salesforceBaseUrl + '/lightning/r/Contact/'+ contactId + '/view';
                        Messaging.SingleEmailMessage emailMessage = new Messaging.SingleEmailMessage();
                        emailMessage.setSubject('New message from '+email);
                        emailMessage.setPlainTextBody(+firstName+ ' ' + lastName + ' is a new contact with this email: ' + email + '\nclick to see the contact: ' +urlContact +'\nMessage: '+ message);
                        //emailMessage.setTargetObjectId(opportunities[0].OwnerId);
                        emailMessage.setTargetObjectId(destination);
                        emailMessage.setSaveAsActivity(false);
                        Messaging.sendEmail(new Messaging.SingleEmailMessage[] { emailMessage });
                        System.debug('envio notificacion');
                        }                   
                            
                                
                            data.put('status', 200);
                            data.put('email', email);
                            data.put('action', 'created');
                            data.put('message', 'New B2C contact created.');
                            String jsonString = JSON.serialize(data);
                            return jsonString;
                            
                            
                            // return 'status: 200, contacto B2C creado';
                        }
                        else
                        {
                            data.put('status', 200);
                            data.put('email', email);
                            data.put('action', 'nothing');
                            data.put('message', 'This email already exist in contacts.');
                            String jsonString = JSON.serialize(data);
                            return jsonString;
                            
                        }
                    }     
                }
            }
         }
            
            
        } catch (Exception ex) {
            // Code to handle the exception goes here
            String mess =  'An error occurred:' +  ex.getMessage() +' '+ex.getStackTraceString()  +' '+  ex.getTypeName();
            data.put('status', 200);
            data.put('email', email);
            data.put('action', 'error');
            data.put('message', mess);
            String jsonString = JSON.serialize(data);
            return jsonString;
            
            
        }
    }
}