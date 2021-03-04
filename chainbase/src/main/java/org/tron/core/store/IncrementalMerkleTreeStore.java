package org.tron.core.store;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.IncrementalMerkleTreeCapsule;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.protos.contract.ShieldContract;

@Slf4j(topic = "DB")
@Component
public class IncrementalMerkleTreeStore
    extends TronStoreWithRevoking<IncrementalMerkleTreeCapsule,
    ShieldContract.IncrementalMerkleTree> {

  @Autowired
  public IncrementalMerkleTreeStore(@Value("IncrementalMerkleTree") String dbName) {
    super(dbName);
  }

  @Override
  public IncrementalMerkleTreeCapsule get(byte[] key) {
    ShieldContract.IncrementalMerkleTree value = revokingDB.getUnchecked(key);
    return value == null
        || value == ShieldContract.IncrementalMerkleTree.getDefaultInstance()
        ? null : new IncrementalMerkleTreeCapsule(value);
  }

  public boolean contain(byte[] key) {
    return revokingDB.has(key);
  }

}
