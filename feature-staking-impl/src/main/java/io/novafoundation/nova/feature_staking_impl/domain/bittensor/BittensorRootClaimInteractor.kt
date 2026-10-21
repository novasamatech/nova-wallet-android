package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_staking_impl.BuildConfig
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicDryRunResult
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorRootClaimSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorRootClaims
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPendingClaims
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorClaimLease
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorClaimTarget
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletConstants
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.ext.ChainGeneses
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
    private val immediateExecution: BittensorImmediateExecution,
    private val pendingClaims: BittensorPendingClaims
) {

    private val options = ExtrinsicService.SubmissionOptions(batchMode = BatchMode.BATCH_ALL)

    fun observe(metaId: Long): Flow<BittensorRootClaimState> = flow {
        val chain = chainRegistry.getChain(ChainGeneses.BITTENSOR)
        emitAll(
            accountRepository.metaAccountFlow(metaId).flatMapLatest { wallet ->
                val coldkey = wallet.requireAccountIdIn(chain)
                val coldkeyKey = AccountIdKey(coldkey)
                val snapshots = rootClaimSource.observe(chain.id, coldkey).onEach { snapshot ->
                    snapshot.claims?.let { pendingClaims.reconcileFreshClaims(chain.id, coldkeyKey, it, snapshot.status) }
                }
                combine(
                    snapshots,
                    pendingClaims.observe(chain.id, coldkeyKey)
                ) { snapshot, pending ->
                    val claims = snapshot.claims
                    if (claims == null) {
                        (snapshot.status as? BittensorDataStatus.Stale)?.let { throw it.cause }
                        null
                    } else {
                        val state = loadState(metaId, null, claims)
                        check(state.coldkey.value.contentEquals(coldkey)) { "The staking wallet has changed" }
                        state.copy(dataStatus = snapshot.status, pendingHotkeys = pending)
                    }
                }.filterNotNull()
            }
        )
    }

    suspend fun load(metaId: Long, selectedHotkey: AccountIdKey? = null): BittensorRootClaimState {
        val state = loadState(metaId, selectedHotkey, null)
        pendingClaims.reconcileFreshClaims(state.chain.id, state.coldkey, state.claims, state.dataStatus)
        return state.copy(pendingHotkeys = pendingClaims.pendingTargets(state.chain.id, state.coldkey))
    }

    private suspend fun loadState(metaId: Long, selectedHotkey: AccountIdKey?, observedClaims: BittensorRootClaims?): BittensorRootClaimState {
        val wallet = accountRepository.getMetaAccount(metaId)
        val chain = chainRegistry.getChain(ChainGeneses.BITTENSOR)
        val coldkey = AccountIdKey(wallet.requireAccountIdIn(chain))
        val asset = walletRepository.assetFlow(wallet.id, chain.utilityAsset)
            .first()
        val claims = observedClaims ?: rootClaimSource.claims(chain.id, coldkey.value)
        val state = BittensorRootClaimState(
            metaId = wallet.id,
            coldkey = coldkey,
            chain = chain,
            asset = asset,
            claims = claims,
            supportsImmediateOperations = immediateExecution.isSupported(wallet),
            selectedHotkey = selectedHotkey
        )
        requireWallet(state, immediate = false)
        return state
    }

    fun prepare(state: BittensorRootClaimState, fee: Fee): BittensorRootClaimDraft? {
        val preview = state.selectedPreview ?: return null
        val operation = BittensorRootClaimOperation(preview.hotkey)
        return BittensorRootClaimDraft(
            state,
            preview,
            operation,
            fee
        )
    }

    suspend fun estimateFee(state: BittensorRootClaimState): Fee? {
        val preview = state.selectedPreview ?: return null
        val operation = BittensorRootClaimOperation(preview.hotkey)
        return estimateFee(state, operation)
    }

    suspend fun validationPayload(draft: BittensorRootClaimDraft): BittensorClaimValidationPayload {
        requireWallet(draft.state)
        val currentClaims = rootClaimSource.claims(draft.state.chain.id, draft.state.coldkey.value)
        pendingClaims.reconcileFreshClaims(draft.state.chain.id, draft.state.coldkey, currentClaims, BittensorDataStatus.Fresh)
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
            safeModeActive = currentClaims.safeModeActive,
            claimPending = draft.preview.hotkey in pendingClaims.pendingTargets(draft.state.chain.id, draft.state.coldkey)
        )
    }

    fun tryAcquire(draft: BittensorRootClaimDraft): BittensorClaimLease? =
        if (draft.state.previewOnly) null else pendingClaims.tryAcquire(draft.target())

    suspend fun dryRun(state: BittensorRootClaimState): ExtrinsicDryRunResult {
        check(BuildConfig.DEBUG && state.previewOnly)
        requireWallet(state)
        val wallet = accountRepository.getMetaAccount(state.metaId)
        if (wallet.type != LightMetaAccount.Type.SECRETS) {
            throw UnsupportedOperationException("Claim dry run currently supports regular signing wallets only")
        }
        val preview = requireNotNull(state.selectedPreview) { "No claim preview is available" }
        val operation = BittensorRootClaimOperation(preview.hotkey)
        return extrinsicService.dryRunExtrinsic(
            state.chain,
            TransactionOrigin.WalletWithId(state.metaId),
            options
        ) { operation.build(this) }
    }

    suspend fun execute(draft: BittensorRootClaimDraft, lease: BittensorClaimLease): BittensorOperationResult {
        require(lease.target == draft.target())
        try {
            check(!draft.state.previewOnly) { "Claim preview cannot be submitted" }
            requireWallet(draft.state)
        } catch (e: CancellationException) {
            pendingClaims.finish(lease, BittensorOperationResult.Failure(e, stage = BittensorFailureStage.NOT_SUBMITTED))
            throw e
        } catch (e: Exception) {
            return e.mapBittensorPreSubmissionError().also { pendingClaims.finish(lease, it) }
        }
        // Marked at handoff, before the existing executor/signing path: no signed callback.
        // Cancellation or an unexpected exception after this point cannot prove non-submission.
        val execution = try {
            submissionExecutor.execute(
                draft.state.metaId,
                draft.state.chain,
                BittensorOutcomeTarget(BittensorOperationKind.CLAIM, BittensorNetuid.Root, setOf(draft.preview.hotkey))
            ) { draft.operation.build(this) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.mapBittensorSubmissionError()
        }
        pendingClaims.finish(lease, execution)
        return execution
    }

    private fun BittensorRootClaimDraft.target() = BittensorClaimTarget(state.chain.id, state.coldkey, preview.hotkey)

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
        if (immediate) immediateExecution.requireSupported(wallet)
        check(
            wallet.id == state.metaId && wallet.requireAccountIdIn(state.chain).contentEquals(state.coldkey.value)
        ) { "The staking wallet has changed" }
    }
}
