package org.tron.core.store;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.MarketAccountOrderCapsule;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.protos.Protocol;

@Component
public class MarketAccountStore extends TronStoreWithRevoking<MarketAccountOrderCapsule,
    Protocol.MarketAccountOrder> {

  @Autowired
  protected MarketAccountStore(@Value("market_account") String dbName) {
    super(dbName);
  }

  @Override
  public MarketAccountOrderCapsule get(byte[] key) throws ItemNotFoundException {
    Protocol.MarketAccountOrder value = revokingDB.get(key);
    return new MarketAccountOrderCapsule(value);
  }

}