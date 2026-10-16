package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorValidatorBackend
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorRootClaimSource
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletConstants
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novasama.substrate_sdk_android.runtime.extrinsic.BatchMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout

class BittensorRootClaimInteractor(
    private val chains: ChainRegistry,
    private val accounts: AccountRepository,
    private val wallets: WalletRepository,
    private val source: BittensorRootClaimSource,
    private val extrinsics: ExtrinsicService,
    private val submission: BittensorSubmissionExecutor,
    private val constants: WalletConstants,
    private val backend: BittensorValidatorBackend
) {
    private val options = ExtrinsicService.SubmissionOptions(batchMode = BatchMode.BATCH_ALL)

    fun observeWalletId() = accounts.selectedMetaAccountFlow().map { it.id }.distinctUntilChanged()

    suspend fun receivedRewards(): BittensorReceivedRootRewards {
        val wallet = accounts.getSelectedMetaAccount()
        val chain = chains.getChain(ChainGeneses.BITTENSOR)
        val coldkey = wallet.requireAccountIdIn(chain)
        val asset = wallets.assetFlow(wallet.id, chain.utilityAsset).first()
        val received = backend.receivedRootRewards(chain.addressOf(coldkey))
        val current = accounts.getSelectedMetaAccount()
        check(current.id == wallet.id && current.requireAccountIdIn(chain).contentEquals(coldkey)) { "The staking wallet changed" }
        return BittensorReceivedRootRewards(asset, received)
    }

    suspend fun load(): BittensorRootClaimState {
        val wallet = accounts.getSelectedMetaAccount()
        val chain = chains.getChain(ChainGeneses.BITTENSOR)
        val coldkey = AccountIdKey(wallet.requireAccountIdIn(chain))
        val asset = wallets.assetFlow(wallet.id, chain.utilityAsset).first()
        val claims = source.claims(chain.id, coldkey.value)
        val state = BittensorRootClaimState(
            wallet.id,
            coldkey,
            chain,
            asset,
            claims,
            wallet.type !=
                io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount.Type.MULTISIG
        )
        requireWallet(state, immediate = false)
        return state
    }

    suspend fun prepare(state: BittensorRootClaimState, hotkey: AccountIdKey): BittensorRootClaimDraft = withTimeout(45_000) {
        requireWallet(state)
        val preview = state.claims.claimable.singleOrNull { it.hotkey == hotkey } ?: error("Rewards are not claimable")
        val operation = BittensorRootClaimOperation(hotkey)
        val fee = extrinsics.estimateFee(state.chain, TransactionOrigin.WalletWithId(state.metaId), options) { operation.build(this) }
        checkFee(state, fee)
        BittensorRootClaimDraft(state, preview, operation, fee)
    }

    suspend fun execute(draft: BittensorRootClaimDraft): BittensorOperationResult {
        requireWallet(draft.state)
        val current = source.claims(draft.state.chain.id, draft.state.coldkey.value)
        val preview = current.claimable.singleOrNull { it.hotkey == draft.operation.hotkey } ?: error("Rewards are no longer claimable")
        require(preview.redeemable >= draft.preview.redeemable) { "Redeemable rewards decreased. Review the claim again" }
        require(preview.forfeitedEstimate <= draft.preview.forfeitedEstimate) { "Estimated forfeiture increased. Review the claim again" }
        val fee = extrinsics.estimateFee(draft.state.chain, TransactionOrigin.WalletWithId(draft.state.metaId), options) { draft.operation.build(this) }
        checkFee(draft.state, fee)
        requireWallet(draft.state)
        return submission.execute(draft.state.metaId, draft.state.chain) { draft.operation.build(this) }
    }

    private suspend fun checkFee(state: BittensorRootClaimState, fee: Fee) {
        val asset = wallets.assetFlow(state.metaId, state.chain.utilityAsset).first()
        require(asset.transferableInPlanks >= fee.maxAmountDeductionFor(asset.token.configuration) + constants.existentialDeposit(state.chain.id)) {
            "Free balance is required for the network fee and minimum account balance"
        }
    }

    private suspend fun requireWallet(state: BittensorRootClaimState, immediate: Boolean = true) {
        val selected = accounts.getSelectedMetaAccount()
        if (immediate) requireBittensorImmediateWallet(selected)
        check(selected.id == state.metaId && selected.requireAccountIdIn(state.chain).contentEquals(state.coldkey.value)) { "The staking wallet has changed" }
    }
}
