package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorRootClaimSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorValidatorBackend
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletConstants
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novasama.substrate_sdk_android.runtime.extrinsic.BatchMode
import kotlinx.coroutines.flow.first

class BittensorRootClaimInteractor(
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
    private val walletRepository: WalletRepository,
    private val rootClaimSource: BittensorRootClaimSource,
    private val extrinsicService: ExtrinsicService,
    private val submissionExecutor: BittensorSubmissionExecutor,
    private val walletConstants: WalletConstants,
    private val validatorBackend: BittensorValidatorBackend
) {

    private val options = ExtrinsicService.SubmissionOptions(batchMode = BatchMode.BATCH_ALL)

    suspend fun receivedRewards(metaId: Long): BittensorReceivedRootRewards {
        val wallet = accountRepository.getMetaAccount(metaId)
        val chain = chainRegistry.getChain(ChainGeneses.BITTENSOR)
        val coldkey = wallet.requireAccountIdIn(chain)
        val asset = walletRepository.assetFlow(wallet.id, chain.utilityAsset)
            .first()
        val received = validatorBackend.receivedRootRewards(chain.addressOf(coldkey))
        return BittensorReceivedRootRewards(asset, received)
    }

    suspend fun load(metaId: Long): BittensorRootClaimState {
        val wallet = accountRepository.getMetaAccount(metaId)
        val chain = chainRegistry.getChain(ChainGeneses.BITTENSOR)
        val coldkey = AccountIdKey(wallet.requireAccountIdIn(chain))
        val asset = walletRepository.assetFlow(wallet.id, chain.utilityAsset)
            .first()
        val claims = rootClaimSource.claims(chain.id, coldkey.value)
        val state = BittensorRootClaimState(
            metaId = wallet.id,
            coldkey = coldkey,
            chain = chain,
            asset = asset,
            claims = claims,
            supportsImmediateOperations = wallet.type != LightMetaAccount.Type.MULTISIG
        )
        requireWallet(state, immediate = false)
        return state
    }

    fun prepare(state: BittensorRootClaimState, fee: Fee): BittensorRootClaimDraft? {
        val preview = state.claims.claimable.firstOrNull() ?: return null
        val operation = BittensorRootClaimOperation(preview.hotkey)
        return BittensorRootClaimDraft(
            state,
            preview,
            operation,
            fee
        )
    }

    suspend fun estimateFee(state: BittensorRootClaimState): Fee? {
        val preview = state.claims.claimable.firstOrNull() ?: return null
        val operation = BittensorRootClaimOperation(preview.hotkey)
        return estimateFee(state, operation)
    }

    suspend fun validationPayload(draft: BittensorRootClaimDraft): BittensorClaimValidationPayload {
        requireWallet(draft.state)
        val currentClaims = rootClaimSource.claims(draft.state.chain.id, draft.state.coldkey.value)
        val currentPreview = currentClaims.claimable.singleOrNull { it.hotkey == draft.operation.hotkey }
        val asset = walletRepository.assetFlow(draft.state.metaId, draft.state.chain.utilityAsset)
            .first()
        val existentialDeposit = walletConstants.existentialDeposit(draft.state.chain.id)
        return BittensorClaimValidationPayload(
            reviewed = draft.preview,
            current = currentPreview,
            transferable = asset.transferableInPlanks,
            fee = draft.fee.maxAmountDeductionFor(asset.token.configuration),
            existentialDeposit = existentialDeposit,
            coldkeySwapPending = currentClaims.coldkeySwapPending,
            safeModeActive = currentClaims.safeModeActive
        )
    }

    suspend fun execute(draft: BittensorRootClaimDraft): BittensorOperationResult {
        requireWallet(draft.state)
        return submissionExecutor.execute(
            draft.state.metaId,
            draft.state.chain
        ) { draft.operation.build(this) }
    }

    private suspend fun estimateFee(state: BittensorRootClaimState, operation: BittensorRootClaimOperation): Fee {
        return extrinsicService.estimateFee(
            state.chain,
            TransactionOrigin.WalletWithId(state.metaId),
            options
        ) { operation.build(this) }
    }

    private suspend fun requireWallet(
        state: BittensorRootClaimState,
        immediate: Boolean = true
    ) {
        val wallet = accountRepository.getMetaAccount(state.metaId)
        if (immediate) requireBittensorImmediateWallet(wallet)
        check(
            wallet.id == state.metaId && wallet.requireAccountIdIn(state.chain).contentEquals(state.coldkey.value)
        ) { "The staking wallet has changed" }
    }
}
