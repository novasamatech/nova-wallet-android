package io.novafoundation.nova.feature_account_migration.presentation.pairing

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.capitalize
import io.novafoundation.nova.feature_account_migration.R
import io.novafoundation.nova.feature_account_migration.domain.AccountMigrationInteractor
import io.novafoundation.nova.feature_account_migration.presentation.AccountMigrationRouter
import io.novafoundation.nova.feature_account_migration.utils.AccountExchangePayload
import io.novafoundation.nova.feature_account_migration.utils.AccountMigrationMixinProvider
import io.novafoundation.nova.feature_account_migration.utils.common.ExchangeSecretsMixin
import io.novafoundation.nova.feature_cloud_backup_api.presenter.mixin.CloudBackupChangingWarningMixinFactory
import io.novasama.substrate_sdk_android.extensions.toHexString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class AccountMigrationPairingViewModel(
    private val resourceManager: ResourceManager,
    private val accountMigrationMixinProvider: AccountMigrationMixinProvider,
    private val accountMigrationInteractor: AccountMigrationInteractor,
    private val payload: AccountMigrationPairingPayload,
    private val router: AccountMigrationRouter,
    private val cloudBackupChangingWarningMixinFactory: CloudBackupChangingWarningMixinFactory
) : BaseViewModel(), Browserable {

    val cloudBackupWarningMixin = cloudBackupChangingWarningMixinFactory.create(this)

    override val openBrowserEvent = MutableLiveData<Event<String>>()

    private val exchangeSecretsMixin = accountMigrationMixinProvider.createAndBindWithScope(this)

    init {
        handleEvents()
    }

    fun acceptMigration() {
        cloudBackupWarningMixin.launchChangingConfirmationIfNeeded {
            exchangeSecretsMixin.acceptKeyExchange()
        }
    }

    private fun handleEvents() {
        exchangeSecretsMixin.exchangeEvents
            .onEach { handleExchangeSecretsEvent(it) }
            .launchIn(this)
    }

    private suspend fun handleExchangeSecretsEvent(event: ExchangeSecretsMixin.ExternalEvent<AccountExchangePayload>) {
        when (event) {
            is ExchangeSecretsMixin.ExternalEvent.SendPublicKey -> {
                val migrationAcceptedUrl = resourceManager.getString(R.string.account_migration_accepted_url, payload.scheme, event.publicKey.toHexString())
                openBrowserEvent.value = Event(migrationAcceptedUrl)
            }

            is ExchangeSecretsMixin.ExternalEvent.PeerSecretsReceived -> {
                val name = event.exchangePayload.accountName ?: fallbackAccountName()
                accountMigrationInteractor.addAccount(name, event.decryptedSecret)

                finishFlow()
            }
        }
    }

    private fun fallbackAccountName(): String {
        return resourceManager.getString(R.string.account_migration_fallback_name, payload.scheme.capitalize())
    }

    private suspend fun finishFlow() {
        if (accountMigrationInteractor.isPinCodeSet()) {
            router.finishMigrationFlow()
        } else {
            router.openPinCodeSet()
        }
    }

    fun close() {
        router.back()
    }
}
