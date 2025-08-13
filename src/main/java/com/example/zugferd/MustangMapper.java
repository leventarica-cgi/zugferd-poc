
package com.example.zugferd;

import org.mustangproject.BankDetails;
import org.mustangproject.Invoice;
import org.mustangproject.Item;
import org.mustangproject.Product;
import org.mustangproject.TradeParty;

import java.math.BigDecimal;
import java.sql.Date;

public final class MustangMapper {
  private MustangMapper() {}

  public static Invoice toZugferd(CanonicalInvoice src) {
    TradeParty sender = new TradeParty(
        src.seller().name(),
        src.seller().street(),
        src.seller().zip(),
        src.seller().city(),
        src.seller().countryCode()
    );
    if (src.seller().vatId() != null && !src.seller().vatId().isBlank()) {
      sender.addVATID(src.seller().vatId());
    }
    if (src.iban() != null && !src.iban().isBlank()) {
      sender.addBankDetails(new BankDetails(src.iban(), src.bic() != null ? src.bic() : ""));
    }

    TradeParty recipient = new TradeParty(
        src.buyer().name(),
        src.buyer().street(),
        src.buyer().zip(),
        src.buyer().city(),
        src.buyer().countryCode()
    );
    if (src.buyer().vatId() != null && !src.buyer().vatId().isBlank()) {
      recipient.addVATID(src.buyer().vatId());
    }

    Invoice inv = new Invoice()
        .setIssueDate(Date.valueOf(src.issueDate()))
        .setDeliveryDate(Date.valueOf(src.issueDate()))
        .setDueDate(Date.valueOf(src.issueDate()))
        .setSender(sender)
        .setRecipient(recipient)
        .setNumber(src.number());

    if (src.notes() != null && !src.notes().isBlank()) {
      inv.setReferenceNumber(src.notes());
    }

    BigDecimal taxPercent = src.taxRate().multiply(BigDecimal.valueOf(100));

    for (LineItem l : src.lines()) {
      Product prod = new Product(l.name(), "", l.unit(), taxPercent);
      Item item = new Item(prod, l.qty(), l.netUnitPrice());
      inv.addItem(item);
    }

    return inv;
  }
}
