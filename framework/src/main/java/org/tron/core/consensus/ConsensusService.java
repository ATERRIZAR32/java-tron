package org.tron.core.consensus;

import com.google.protobuf.ByteString;
import com.sun.org.apache.xpath.internal.Arg;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.SignUtils;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.DBConfig;
import org.tron.consensus.Consensus;
import org.tron.consensus.base.Param;
import org.tron.consensus.base.Param.Miner;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.store.WitnessStore;

@Slf4j(topic = "consensus")
@Component
public class ConsensusService {

  @Autowired
  private Consensus consensus;

  @Autowired
  private WitnessStore witnessStore;

  @Autowired
  private BlockHandleImpl blockHandle;

  private Args args = Args.getInstance();

  public void start() {
    Param param = new Param();
    param.setEnable(args.isWitness());
    param.setGenesisBlock(args.getGenesisBlock());
    param.setMinParticipationRate(args.getMinParticipationRate());
    param.setBlockProduceTimeoutPercent(Args.getInstance().getBlockProducedTimeOut());
    param.setNeedSyncCheck(args.isNeedSyncCheck());
    List<Miner> miners = new ArrayList<>();
    List<String> privateKeys = args.getLocalWitnesses().getPrivateKeys();
    for (String privateKey: privateKeys) {
      byte[] privateKeyAddress = ECKey.fromPrivate(ByteArray.fromHexString(privateKey)).getAddress();
      WitnessCapsule witnessCapsule = witnessStore.get(privateKeyAddress);
      if (null == witnessCapsule) {
        logger.warn("Witness {} is not in witnessStore.", Hex.encodeHexString(privateKeyAddress));
      }
      Miner miner = param.new Miner(ByteArray.fromHexString(privateKey), ByteString.copyFrom(privateKeyAddress),
        ByteString.copyFrom(privateKeyAddress));
      miners.add(miner);
    }
    logger.info("Total {} miners.", miners.size());
    param.setMiners(miners);
    param.setBlockHandle(blockHandle);
    consensus.start(param);
  }

  public void stop() {
    consensus.stop();
  }

}
