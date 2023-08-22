trigger OrderApex on Order (after insert, after update, after delete, after undelete) {
    System.debug('entro order trigger');
    
 //   System.debug('total amountnew: '+Trigger.new[0].totalamount);
 //   System.debug('total amountold: '+Trigger.old[0].totalamount);
    
    Set<Id> accountIds = new Set<Id>();
    
    // Collect Account IDs based on the Orders being modified
    if (Trigger.isInsert || Trigger.isUpdate || Trigger.isUndelete) {
        for (Order order : Trigger.new) {
            accountIds.add(order.AccountId);
            System.debug('accountID: '+order.AccountId);
            
        }
    } else if (Trigger.isDelete) {
        for (Order order : Trigger.old) {
            accountIds.add(order.AccountId);
        }
    }
    
    
    // Query the total sum of TotalAmount for the related Orders
    List<Account> accountsToUpdate = new List<Account>();
    for (AggregateResult aggregateResult : [
        SELECT AccountId, SUM(TotalAmount) total
        FROM Order
        WHERE AccountId IN :accountIds
        GROUP BY AccountId
    ]) {
        Id accountId = (Id)aggregateResult.get('AccountId');
        Decimal totalAmount = (Decimal)aggregateResult.get('total');
        
        Account accountToUpdate = new Account(Id = accountId);
        accountToUpdate.TotalAmount__c = totalAmount != null ? totalAmount : 0;
        if (Trigger.isInsert) {
            System.debug('Trigger.isInsert');
            System.debug('Trigger.isInsert totalAmount: '+ totalAmount);
        }
        if (Trigger.isUpdate) {
            System.debug('Trigger.isUpdate');
            System.debug(' Trigger.isUpdate totalAmount: '+ totalAmount);
        }
        accountsToUpdate.add(accountToUpdate);
    }
    
    // Update the TotalAmount__c field on the related Account records
    update accountsToUpdate;
    
}