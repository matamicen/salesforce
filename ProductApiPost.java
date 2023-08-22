@RestResource(urlMapping='/productApiPost/*')
global with sharing class ProductApiPost {
    
    @HttpPost
    global static String createProduct(String project_id, String project, String project_description, String unit, String country_iso2,  String country, String project_kind, String project_type, String standard_type, String category) {
        // Data for JSON
        Map<String, Object> data = new Map<String, Object>();
        
        try {
            List<Product2> existingProducts = [SELECT Id FROM Product2 WHERE ProductCode = :project_id LIMIT 1];
            
            if (!existingProducts.isEmpty()) {
                Product2 existingProduct = existingProducts[0];
                // Update existing product
                existingProduct.Name = project;
                existingProduct.Description = project_description;
                existingProduct.QuantityUnitOfMeasure = unit;
                existingProduct.Family = project_kind;
                // Update other fields as needed
                
                try {
                    update existingProduct;
                    data.put('status', 200);
                    data.put('project', project);
                    data.put('action', 'updated');
                    data.put('message', 'Product '+ project_id +' updated successfully');
                } catch (Exception e) {
                    data.put('status', 500);
                    data.put('project', project);
                    data.put('action', 'error');
                    data.put('message', 'Error updating product: ' + project_id + ' ' + e.getMessage());
                }
            } else {
                // Create new product
                Product2 newProduct = new Product2();
                newProduct.Name = project;
                newProduct.Description = project_description;
                newProduct.ProductCode = project_id;
                newProduct.CurrencyIsoCode = 'EUR';
                newProduct.QuantityUnitOfMeasure = unit;
                newProduct.Family = project_kind;
                newProduct.IsActive = true;
     
                try {
                    insert newProduct;
                    
                    // Assign Standard Price to Pricebook Entry
                   // Pricebook2 standardPricebook = [SELECT Id FROM Pricebook2 WHERE IsStandard = true LIMIT 1];
                   // 
                    List<Pricebook2> standardPricebook = [SELECT Id FROM Pricebook2 WHERE name = 'Standard Price Book' LIMIT 1];
                    PricebookEntry newPricebookEntry = new PricebookEntry();
                    newPricebookEntry.Product2Id = newProduct.Id;
                    newPricebookEntry.Pricebook2Id = standardPricebook[0].Id;
                    newPricebookEntry.UnitPrice = 1.00; // Set the standard price
                    newPricebookEntry.CurrencyIsoCode = 'EUR';
                    newPricebookEntry.IsActive = true;
                    insert newPricebookEntry;
                    
                    data.put('status', 200);
                    data.put('project', project);
                    data.put('action', 'created');
                    data.put('message', 'Product '+project_id +' created successfully');
                } catch (Exception e) {
                    data.put('status', 500);
                    data.put('project', project);
                    data.put('action', 'error');
                    data.put('message', 'Error creating product: '+ project_id +' ' + e.getMessage());
                }
            }
        } catch (Exception ex) {
            data.put('status', 500);
            data.put('project', project);
            data.put('action', 'error');
            data.put('message', 'An error occurred: ' + ex.getMessage());
        }
        
        return JSON.serialize(data);
    }
}