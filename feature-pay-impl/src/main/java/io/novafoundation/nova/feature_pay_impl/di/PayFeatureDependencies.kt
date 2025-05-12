package io.novafoundation.nova.feature_pay_impl.di

import coil.ImageLoader
import com.google.gson.Gson
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.data.storage.encrypt.EncryptedPreferences
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.NetworkStateService
import io.novafoundation.nova.feature_account_api.data.secrets.AccountSecretsFactory
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_wallet_connect_api.presentation.mixin.WalletConnectSessionsMixinFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

interface PayFeatureDependencies {

    val selectedAccountUseCase: SelectedAccountUseCase

    val walletConnectSessionsMixinFactory: WalletConnectSessionsMixinFactory

    val accountRepository: AccountRepository

    val networkStateService: NetworkStateService

    val gson: Gson

    val networkApiCreator: NetworkApiCreator

    val accountSecretsFactory: AccountSecretsFactory

    val secretsStoreV2: SecretStoreV2

    val encryptedPreferences: EncryptedPreferences

    val chainRegistry: ChainRegistry

    val imageLoader: ImageLoader

    val resourceManager: ResourceManager
}
