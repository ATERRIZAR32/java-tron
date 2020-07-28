package org.tron.core.vm.nativecontract;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.utils.StringUtil;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.exception.BalanceInsufficientException;
import org.tron.core.store.DelegationStore;
import org.tron.core.vm.repository.RepositoryImpl;
import org.tron.protos.Protocol;

@Slf4j(topic = "contractService")
public class ContractService {

    private RepositoryImpl repository;

    public ContractService(RepositoryImpl repository) {
        this.repository = repository;
    }

    public void withdrawReward(byte[] address) {
        if (!repository.getDynamicPropertiesStore().allowChangeDelegation()) {
            return;
        }
        AccountCapsule accountCapsule = repository.getAccount(address);
        long beginCycle = repository.getBeginCycle(address);
        long endCycle = repository.getEndCycle(address);
        long currentCycle = repository.getDynamicPropertiesStore().getCurrentCycleNumber();
        long reward = 0;
        if (beginCycle > currentCycle || accountCapsule == null) {
            return;
        }
        if (beginCycle == currentCycle) {
            AccountCapsule account = repository.getAccountVote(beginCycle, address);
            if (account != null) {
                return;
            }
        }
        //withdraw the latest cycle reward
        if (beginCycle + 1 == endCycle && beginCycle < currentCycle) {
            AccountCapsule account = repository.getAccountVote(beginCycle, address);
            if (account != null) {
                reward = computeReward(beginCycle, account);
                try {
                    adjustAllowance(address, reward);
                } catch (BalanceInsufficientException e) {
                    logger.error("withdrawReward error: {},{}", Hex.toHexString(address), address, e);
                }
                reward = 0;
                logger.info("latest cycle reward {},{}", beginCycle, account.getVotesList());
            }
            beginCycle += 1;
        }
        //
        endCycle = currentCycle;
        if (CollectionUtils.isEmpty(accountCapsule.getVotesList())) {
            repository.upRemark(address, endCycle);
            repository.updateBeginCycle(address, endCycle + 1);
            return;
        }
        if (beginCycle < endCycle) {
            for (long cycle = beginCycle; cycle < endCycle; cycle++) {
                reward += computeReward(cycle, accountCapsule);
            }
            try {
                adjustAllowance(address, reward);
            } catch (BalanceInsufficientException e) {
                logger.error("withdrawReward error: {},{}", Hex.toHexString(address), address, e);
            }
        }
        repository.updateBeginCycle(address, endCycle);
        repository.updateEndCycle(address, endCycle + 1);
        repository.updateAccountVote(address,endCycle,  accountCapsule);
        repository.commit();
        logger.info("adjust {} allowance {}, now currentCycle {}, beginCycle {}, endCycle {}, "
                        + "account vote {},", Hex.toHexString(address), reward, currentCycle,
                beginCycle, endCycle, accountCapsule.getVotesList());
    }

    private long computeReward(long cycle, AccountCapsule accountCapsule) {
        long reward = 0;

        for (Protocol.Vote vote : accountCapsule.getVotesList()) {
            byte[] srAddress = vote.getVoteAddress().toByteArray();
            long totalReward = repository.getDelegationStore().getReward(cycle,
                    srAddress);
            long totalVote = repository.getDelegationStore().getWitnessVote(cycle, srAddress);
            if (totalVote == DelegationStore.REMARK || totalVote == 0) {
                continue;
            }
            long userVote = vote.getVoteCount();
            double voteRate = (double) userVote / totalVote;
            reward += voteRate * totalReward;
            logger.debug("computeReward {} {} {} {},{},{},{}", cycle,
                    Hex.toHexString(accountCapsule.getAddress().toByteArray()), Hex.toHexString(srAddress),
                    userVote, totalVote, totalReward, reward);
        }
        return reward;
    }

    public void adjustAllowance(byte[] address, long amount) throws BalanceInsufficientException {
        if (amount <= 0) {
            return;
        }
        AccountCapsule accountCapsule = repository.getAccount(address);
        long allowance = accountCapsule.getAllowance();
        if (amount == 0) {
            return;
        }
        if (amount < 0 && allowance < -amount) {
            throw new BalanceInsufficientException(
                    StringUtil.createReadableString(address) + " insufficient balance");
        }
        accountCapsule.setAllowance(allowance + amount);
        repository.putAccountValue(accountCapsule.createDbKey(), accountCapsule);
    }
}
