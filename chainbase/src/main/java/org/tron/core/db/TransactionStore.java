package org.tron.core.db;

import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.KhaosDatabase.KhaosBlock;
import org.tron.core.exception.BadItemException;
import org.tron.protos.Protocol.CrossMessage;

@Slf4j(topic = "DB")
@Component
public class TransactionStore extends TronStoreWithRevoking<TransactionCapsule> {

  @Autowired
  private BlockStore blockStore;

  @Autowired
  private KhaosDatabase khaosDatabase;

  @Autowired
  private TransactionStore(@Value("trans") String dbName) {
    super(dbName);
  }

  @Override
  public void put(byte[] key, TransactionCapsule item) {
    if (Objects.isNull(item) || item.getBlockNum() == -1) {
      super.put(key, item);
    } else {
      revokingDB.put(key, ByteArray.fromLong(item.getBlockNum()));
    }
  }

  private TransactionCapsule getTransactionFromBlockStore(byte[] key, long blockNum) {
    List<BlockCapsule> blocksList = blockStore.getLimitNumber(blockNum, 1);
    if (blocksList.size() != 0) {
      for (TransactionCapsule e : blocksList.get(0).getTransactions()) {
        if (e.getTransactionId().equals(Sha256Hash.wrap(key))) {
          return e;
        }
      }
      for (CrossMessage crossMessage : blocksList.get(0).getCrossMessageList()) {
        TransactionCapsule tx = new TransactionCapsule(crossMessage.getTransaction());
        if (tx.getTransactionId().equals(Sha256Hash.wrap(key))) {
          return tx;
        }
      }
    }
    return null;
  }

  private TransactionCapsule getTransactionFromKhaosDatabase(byte[] key, long high) {
    List<KhaosBlock> khaosBlocks = khaosDatabase.getMiniStore().getBlockByNum(high);
    for (KhaosBlock bl : khaosBlocks) {
      for (TransactionCapsule e : bl.getBlk().getTransactions()) {
        if (e.getTransactionId().equals(Sha256Hash.wrap(key))) {
          return e;
        }
      }
      for (CrossMessage crossMessage : bl.getBlk().getCrossMessageList()) {
        TransactionCapsule tx = new TransactionCapsule(crossMessage.getTransaction());
        if (tx.getTransactionId().equals(Sha256Hash.wrap(key))) {
          return tx;
        }
      }
    }
    return null;
  }

  public long getBlockNumber(byte[] key) throws BadItemException {
    byte[] value = revokingDB.getUnchecked(key);
    if (ArrayUtils.isEmpty(value)) {
      return -1;
    }

    if (value.length == 8) {
      return ByteArray.toLong(value);
    }
    TransactionCapsule transactionCapsule = new TransactionCapsule(value);
    return transactionCapsule.getBlockNum();
  }

  @Override
  public TransactionCapsule get(byte[] key) throws BadItemException {
    byte[] value = revokingDB.getUnchecked(key);
    if (ArrayUtils.isEmpty(value)) {
      return null;
    }
    TransactionCapsule transactionCapsule = null;
    if (value.length == 8) {
      long blockHigh = ByteArray.toLong(value);
      transactionCapsule = getTransactionFromBlockStore(key, blockHigh);
      if (transactionCapsule == null) {
        transactionCapsule = getTransactionFromKhaosDatabase(key, blockHigh);
      }
      transactionCapsule.setBlockNum(blockHigh);
    }

    return transactionCapsule == null ? new TransactionCapsule(value) : transactionCapsule;
  }

  @Override
  public TransactionCapsule getUnchecked(byte[] key) {
    try {
      return get(key);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * get total transaction.
   */
  @Deprecated
  public long getTotalTransactions() {
    return 0; //Streams.stream(iterator()).count();
  }

  public CrossMessage getCrossMessage(byte[] key) {
    byte[] value = revokingDB.getUnchecked(key);
    if (ArrayUtils.isEmpty(value)) {
      return null;
    }
    if (value.length == 8) {
      long blockHigh = ByteArray.toLong(value);
      List<BlockCapsule> blocksList = blockStore.getLimitNumber(blockHigh, 1);
      if (blocksList.size() != 0) {
        for (CrossMessage crossMessage : blocksList.get(0).getCrossMessageList()) {
          TransactionCapsule tx = new TransactionCapsule(crossMessage.getTransaction());
          if (tx.getTransactionId().equals(Sha256Hash.wrap(key))) {
            return crossMessage;
          }
        }
      }
      List<KhaosBlock> khaosBlocks = khaosDatabase.getMiniStore().getBlockByNum(blockHigh);
      for (KhaosBlock bl : khaosBlocks) {
        for (CrossMessage crossMessage : bl.getBlk().getCrossMessageList()) {
          TransactionCapsule tx = new TransactionCapsule(crossMessage.getTransaction());
          if (tx.getTransactionId().equals(Sha256Hash.wrap(key))) {
            return crossMessage;
          }
        }
      }
    }
    return null;
  }
}
