package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.analytics.AnalyticsService
import io.novafoundation.nova.analytics.AnalyticsWalletType
import io.novafoundation.nova.analytics.BittensorAnalyticsEvent
import io.novafoundation.nova.analytics.StakingAnalyticsTarget
import io.novafoundation.nova.analytics.StakingBlockedReason
import io.novafoundation.nova.analytics.StakingFlow
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
import javax.inject.Inject

/**
 * The iOS `checkingSupport(of: .subtensorStaking)` gate in front of a TAO staking flow: a Ledger or proxied wallet, or a multisig
 * whose calls execute with a delay, gets the unsupported-wallet sheet and one `staking_blocked` instead of the flow.
 */
class BittensorWalletGate @Inject constructor(
    private val immediateExecution: BittensorImmediateExecution,
    private val accountRepository: AccountRepository,
    private val resources: ResourceManager,
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
            BittensorWalletSupport.LEDGER_NOT_SUPPORTED -> StakingBlockedReason.LEDGER
            BittensorWalletSupport.PROXIED_NOT_SUPPORTED -> StakingBlockedReason.PROXY
            BittensorWalletSupport.DELAYED_EXECUTION -> StakingBlockedReason.MULTISIG_UNSUPPORTED
        }

        analytics.track(BittensorAnalyticsEvent.Blocked(flow, refusal, wallet.type.analyticsWalletType(), target))
        onRefusal()
        showRefusal(refusal)
    }

    // As iOS presentOperationCompletion: Ledger gets the signer-not-supported sheet, proxied and delayed multisig wallets the
    // unsupported-wallet sheet, each with "Okay, back" only.
    private fun showRefusal(reason: StakingBlockedReason) {
        val (imageRes, title, subtitle) = when (reason) {
            StakingBlockedReason.LEDGER -> Triple(
                R.drawable.ic_ledger,
                resources.getString(R.string.account_parity_signer_not_supported_title),
                resources.getString(R.string.ledger_sign_raw_not_supported)
            )
            StakingBlockedReason.PROXY -> Triple(R.drawable.ic_proxy, unsupportedTitle(R.string.account_proxied), unsupportedMessage())
            else -> Triple(R.drawable.ic_multisig, unsupportedTitle(R.string.account_multisig), unsupportedMessage())
        }

        launchBottomSheet(
            imageRes = imageRes,
            title = title,
            subtitle = subtitle,
            actionButtonPreferences = ButtonPreferences.primary(resources.getString(R.string.common_ok_back)),
            neutralButtonPreferences = null
        )
    }

    private fun unsupportedTitle(walletType: Int) =
        resources.getString(R.string.bittensor_staking_wallet_unsupported_title, resources.getString(walletType))

    private fun unsupportedMessage() = resources.getString(R.string.bittensor_staking_wallet_unsupported_message)
}

/** iOS `MetaAccountModelType.analyticsWalletType`. */
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
