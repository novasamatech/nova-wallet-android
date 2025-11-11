package io.novafoundation.nova.feature_account_impl.di

import io.novafoundation.nova.common.di.FeatureApiHolder
import io.novafoundation.nova.common.di.FeatureContainer
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.common.sequrity.verification.PinCodeTwoFactorVerificationCommunicator
import io.novafoundation.nova.core_db.di.DbApi
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectMultipleWalletsCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.changePassword.ChangeBackupPasswordCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.changePassword.RestoreBackupPasswordCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.createPassword.SyncWalletsBackupPasswordCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectAddress.SelectAddressCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectSingleWallet.SelectSingleWalletCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet.SelectWalletCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.sign.LedgerSignCommunicator
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.PolkadotVaultVariantSignCommunicator
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_cloud_backup_api.di.CloudBackupFeatureApi
import io.novafoundation.nova.feature_currency_api.di.CurrencyFeatureApi
import io.novafoundation.nova.feature_ledger_core.di.LedgerCoreApi
import io.novafoundation.nova.feature_proxy_api.di.ProxyFeatureApi
import io.novafoundation.nova.feature_swap_core_api.di.SwapCoreApi
import io.novafoundation.nova.feature_versions_api.di.VersionsFeatureApi
import io.novafoundation.nova.feature_xcm_api.di.XcmFeatureApi
import io.novafoundation.nova.runtime.di.RuntimeApi
import io.novafoundation.nova.web3names.di.Web3NamesApi
import javax.inject.Inject

@ApplicationScope
class AccountFeatureHolder @Inject constructor(
    featureContainer: FeatureContainer,
    private val accountRouter: AccountRouter,
    private val polkadotVaultSignCommunicator: PolkadotVaultVariantSignCommunicator,
    private val ledgerSignCommunicator: LedgerSignCommunicator,
    private val selectAddressCommunicator: SelectAddressCommunicator,
    private val selectMultipleWalletsCommunicator: SelectMultipleWalletsCommunicator,
    private val selectWalletCommunicator: SelectWalletCommunicator,
    private val pinCodeTwoFactorVerificationCommunicator: PinCodeTwoFactorVerificationCommunicator,
    private val syncWalletsBackupPasswordCommunicator: SyncWalletsBackupPasswordCommunicator,
    private val changeBackupPasswordCommunicator: ChangeBackupPasswordCommunicator,
    private val restoreBackupPasswordCommunicator: RestoreBackupPasswordCommunicator,
    private val selectSingleWalletCommunicator: SelectSingleWalletCommunicator,
) : FeatureApiHolder(featureContainer) {

    override fun initializeDependencies(): Any {
        val accountFeatureDependencies = DaggerAccountFeatureComponent_AccountFeatureDependenciesComponent.builder()
            .commonApi(commonApi())
            .dbApi(getFeature(DbApi::class.java))
            .runtimeApi(getFeature(RuntimeApi::class.java))
            .currencyFeatureApi(getFeature(CurrencyFeatureApi::class.java))
            .proxyFeatureApi(getFeature(ProxyFeatureApi::class.java))
            .versionsFeatureApi(getFeature(VersionsFeatureApi::class.java))
            .web3NamesApi(getFeature(Web3NamesApi::class.java))
            .cloudBackupFeatureApi(getFeature(CloudBackupFeatureApi::class.java))
            .ledgerCoreApi(getFeature(LedgerCoreApi::class.java))
            .swapCoreApi(getFeature(SwapCoreApi::class.java))
            .xcmFeatureApi(getFeature(XcmFeatureApi::class.java))
            .build()

        return DaggerAccountFeatureComponent.factory()
            .create(
                accountRouter = accountRouter,
                polkadotVaultSignInterScreenCommunicator = polkadotVaultSignCommunicator,
                ledgerSignInterScreenCommunicator = ledgerSignCommunicator,
                selectAddressCommunicator = selectAddressCommunicator,
                selectMultipleWalletsCommunicator = selectMultipleWalletsCommunicator,
                selectWalletCommunicator = selectWalletCommunicator,
                pinCodeTwoFactorVerificationCommunicator = pinCodeTwoFactorVerificationCommunicator,
                syncWalletsBackupPasswordCommunicator = syncWalletsBackupPasswordCommunicator,
                changeBackupPasswordCommunicator = changeBackupPasswordCommunicator,
                restoreBackupPasswordCommunicator = restoreBackupPasswordCommunicator,
                selectSingleWalletCommunicator = selectSingleWalletCommunicator,
                deps = accountFeatureDependencies
            )
    }
}
