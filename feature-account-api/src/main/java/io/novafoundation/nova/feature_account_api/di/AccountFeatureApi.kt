package io.novafoundation.nova.feature_account_api.di

import io.novafoundation.nova.common.sequrity.TwoFactorVerificationExecutor
import io.novafoundation.nova.common.sequrity.biometry.BiometricServiceFactory
import io.novafoundation.nova.feature_account_api.data.cloudBackup.LocalAccountsCloudBackupFacade
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.EvmTransactionService
import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentProviderRegistry
import io.novafoundation.nova.feature_account_api.data.fee.capability.CustomFeeCapabilityFacade
import io.novafoundation.nova.feature_account_api.data.fee.types.hydra.HydrationFeeInjector
import io.novafoundation.nova.feature_account_api.data.proxy.ProxySyncService
import io.novafoundation.nova.feature_account_api.data.proxy.validation.ProxyExtrinsicValidationRequestBus
import io.novafoundation.nova.feature_account_api.data.repository.OnChainIdentityRepository
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.ledger.GenericLedgerAddAccountRepository
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.ledger.LegacyLedgerAddAccountRepository
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.secrets.MnemonicAddAccountRepository
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.data.signer.SigningSharedState
import io.novafoundation.nova.feature_account_api.domain.account.common.EncryptionDefaults
import io.novafoundation.nova.feature_account_api.domain.account.identity.IdentityProvider
import io.novafoundation.nova.feature_account_api.domain.account.identity.LocalIdentity
import io.novafoundation.nova.feature_account_api.domain.account.identity.OnChainIdentity
import io.novafoundation.nova.feature_account_api.domain.cloudBackup.ApplyLocalSnapshotToCloudBackupUseCase
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.MetaAccountGroupingInteractor
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.common.listing.MetaAccountTypePresentationMapper
import io.novafoundation.nova.feature_account_api.presenatation.account.copyAddress.CopyAddressMixin
import io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.config.PolkadotVaultVariantConfigProvider
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.watchOnly.WatchOnlyMissingKeysPresenter
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.addressActions.AddressActionsMixin
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.createPassword.SyncWalletsBackupPasswordCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.language.LanguageUseCase
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.AddressInputMixinFactory
import io.novafoundation.nova.feature_account_api.presenatation.mixin.identity.IdentityMixin
import io.novafoundation.nova.feature_account_api.presenatation.mixin.importType.ImportTypeChooserMixin
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectAddress.SelectAddressMixin
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet.SelectWalletMixin

interface AccountFeatureApi {

    val addressInputMixinFactory: AddressInputMixinFactory

    val walletUiUseCase: WalletUiUseCase

    val signerProvider: SignerProvider

    val watchOnlyMissingKeysPresenter: WatchOnlyMissingKeysPresenter

    val signSharedState: SigningSharedState

    val onChainIdentityRepository: OnChainIdentityRepository

    val metaAccountTypePresentationMapper: MetaAccountTypePresentationMapper

    val legacyLedgerAddAccountRepository: LegacyLedgerAddAccountRepository

    val genericLedgerAddAccountRepository: GenericLedgerAddAccountRepository

    val evmTransactionService: EvmTransactionService

    val identityMixinFactory: IdentityMixin.Factory

    val languageUseCase: LanguageUseCase

    val selectWalletMixinFactory: SelectWalletMixin.Factory

    val polkadotVaultVariantConfigProvider: PolkadotVaultVariantConfigProvider

    val selectAddressMixinFactory: SelectAddressMixin.Factory

    val metaAccountChangesEventBus: MetaAccountChangesEventBus

    val applyLocalSnapshotToCloudBackupUseCase: ApplyLocalSnapshotToCloudBackupUseCase

    val feePaymentProviderRegistry: FeePaymentProviderRegistry

    val customFeeCapabilityFacade: CustomFeeCapabilityFacade

    val hydrationFeeInjector: HydrationFeeInjector

    val addressActionsMixinFactory: AddressActionsMixin.Factory

    val mnemonicAddAccountRepository: MnemonicAddAccountRepository

    @LocalIdentity
    fun localIdentityProvider(): IdentityProvider

    @OnChainIdentity
    fun onChainIdentityProvider(): IdentityProvider

    fun proxySyncService(): ProxySyncService

    fun metaAccountGroupingInteractor(): MetaAccountGroupingInteractor

    fun accountInteractor(): AccountInteractor

    fun provideAccountRepository(): AccountRepository

    fun externalAccountActions(): ExternalActions.Presentation

    fun accountUpdateScope(): AccountUpdateScope

    fun addressDisplayUseCase(): AddressDisplayUseCase

    fun accountUseCase(): SelectedAccountUseCase

    fun extrinsicService(): ExtrinsicService

    fun extrinsicServiceFactory(): ExtrinsicService.Factory

    fun importTypeChooserMixin(): ImportTypeChooserMixin.Presentation

    fun twoFactorVerificationExecutor(): TwoFactorVerificationExecutor

    fun biometricServiceFactory(): BiometricServiceFactory

    fun encryptionDefaults(): EncryptionDefaults

    fun proxyExtrinsicValidationRequestBus(): ProxyExtrinsicValidationRequestBus

    fun cloudBackupFacade(): LocalAccountsCloudBackupFacade

    fun syncWalletsBackupPasswordCommunicator(): SyncWalletsBackupPasswordCommunicator

    fun copyAddressMixin(): CopyAddressMixin
}
