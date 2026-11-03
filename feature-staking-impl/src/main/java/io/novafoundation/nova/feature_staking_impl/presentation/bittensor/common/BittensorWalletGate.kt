package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.novafoundation.nova.analytics.AnalyticsService
import io.novafoundation.nova.analytics.AnalyticsWalletType
import io.novafoundation.nova.analytics.BittensorAnalyticsEvent
import io.novafoundation.nova.analytics.StakingAnalyticsTarget
import io.novafoundation.nova.analytics.StakingBlockedReason
import io.novafoundation.nova.analytics.StakingFlow
import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncher
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncherFactory
import io.novafoundation.nova.common.view.bottomSheet.action.ButtonPreferences
import io.novafoundation.nova.common.view.bottomSheet.action.primary
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorImmediateExecution
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorWalletSupport
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import javax.inject.Inject

/**
 * The gate in front of a TAO staking flow: a proxied wallet, or a multisig whose calls
 * execute with a delay, gets the unsupported-wallet sheet and one `staking_blocked` instead of the flow.
 */
class BittensorWalletGate @Inject constructor(
    private val immediateExecution: BittensorImmediateExecution,
    private val accountRepository: AccountRepository,
    private val chainRegistry: ChainRegistry,
    private val resourceManager: ResourceManager,
    private val analytics: AnalyticsService,
    actionBottomSheetLauncherFactory: ActionBottomSheetLauncherFactory
) : ActionBottomSheetLauncher by actionBottomSheetLauncherFactory.create() {

    suspend fun checkingSupport(metaId: Long, flow: StakingFlow, target: StakingAnalyticsTarget?, onSupported: () -> Unit) {
        checkingSupport(accountRepository.getMetaAccount(metaId), flow, target, onSupported = onSupported)
    }

    /** Runs [onSupported] for a supported [wallet]; otherwise shows the sheet, reports [flow] and [target] and runs [onRefusal]. */
    suspend fun checkingSupport(
        wallet: MetaAccount,
        flow: StakingFlow,
        target: StakingAnalyticsTarget?,
        onRefusal: () -> Unit = {},
        onSupported: () -> Unit
    ) {
        val refusal = when (immediateExecution.support(wallet)) {
            BittensorWalletSupport.SUPPORTED -> return onSupported()
            BittensorWalletSupport.PROXIED_NOT_SUPPORTED -> StakingBlockedReason.PROXY
            BittensorWalletSupport.DELAYED_EXECUTION -> StakingBlockedReason.MULTISIG_UNSUPPORTED
        }

        analytics.track(BittensorAnalyticsEvent.Blocked(flow, refusal, wallet.type.analyticsWalletType(), target))
        onRefusal()

        val nativeSymbol = chainRegistry.getChain(ChainGeneses.BITTENSOR).utilityAsset.symbol.value
        when (refusal) {
            StakingBlockedReason.PROXY -> showWarning(R.drawable.ic_proxy, resourceManager.bittensorWalletNotSupported(R.string.account_proxied, nativeSymbol))
            else -> showWarning(R.drawable.ic_multisig, resourceManager.bittensorWalletNotSupported(R.string.account_multisig, nativeSymbol))
        }
    }

    private fun showWarning(@DrawableRes imageRes: Int, warning: TitleAndMessage) {
        val (title, message) = warning
        launchBottomSheet(
            imageRes = imageRes,
            title = title,
            subtitle = message ?: "",
            actionButtonPreferences = ButtonPreferences.primary(resourceManager.getString(R.string.common_ok_back)),
            neutralButtonPreferences = null
        )
    }
}

fun ResourceManager.bittensorWalletNotSupported(@StringRes walletType: Int, nativeSymbol: String): TitleAndMessage =
    getString(R.string.bittensor_staking_wallet_unsupported_title, getString(walletType)) to
        getString(R.string.bittensor_staking_wallet_unsupported_message, nativeSymbol)

/** A watch-only wallet is reported blocked once its stake confirmation opens. */
fun AnalyticsService.reportWatchOnlyStake(target: StakingAnalyticsTarget) {
    track(BittensorAnalyticsEvent.Blocked(StakingFlow.STAKE, StakingBlockedReason.WATCH_ONLY, AnalyticsWalletType.WATCH_ONLY, target))
}

private fun LightMetaAccount.Type.analyticsWalletType(): AnalyticsWalletType = when (this) {
    LightMetaAccount.Type.SECRETS -> AnalyticsWalletType.SECRETS
    LightMetaAccount.Type.WATCH_ONLY -> AnalyticsWalletType.WATCH_ONLY
    LightMetaAccount.Type.PARITY_SIGNER -> AnalyticsWalletType.PARITY_SIGNER
    LightMetaAccount.Type.LEDGER_LEGACY -> AnalyticsWalletType.LEDGER
    LightMetaAccount.Type.LEDGER -> AnalyticsWalletType.GENERIC_LEDGER
    LightMetaAccount.Type.POLKADOT_VAULT -> AnalyticsWalletType.POLKADOT_VAULT
    LightMetaAccount.Type.PROXIED -> AnalyticsWalletType.PROXIED
    LightMetaAccount.Type.MULTISIG -> AnalyticsWalletType.MULTISIG
}
