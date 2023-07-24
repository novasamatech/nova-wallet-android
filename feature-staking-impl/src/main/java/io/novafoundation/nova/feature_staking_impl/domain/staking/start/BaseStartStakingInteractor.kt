package io.novafoundation.nova.feature_staking_impl.domain.staking.start

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope

abstract class BaseStartStakingInteractor(
    internal val stakingType: Chain.Asset.StakingType,
    internal val accountRepository: AccountRepository,
    internal val walletRepository: WalletRepository,
    internal val coroutineScope: CoroutineScope,
) : StartStakingInteractor
