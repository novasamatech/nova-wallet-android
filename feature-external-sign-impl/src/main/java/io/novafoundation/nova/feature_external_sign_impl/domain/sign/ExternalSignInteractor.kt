package io.novafoundation.nova.feature_external_sign_impl.domain.sign

import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.feature_external_sign_api.model.ExternalSignCommunicator
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import kotlinx.coroutines.flow.Flow

interface ExternalSignInteractor {

    sealed class Error : Throwable() {
        class UnsupportedChain(val chainId: String) : Error()
    }

    val validationSystem: ConfirmDAppOperationValidationSystem

    suspend fun createAccountAddressModel(): AddressModel

    suspend fun chainUi(): Result<ChainUi?>

    fun commissionTokenFlow(): Flow<Token?>?

    suspend fun calculateFee(): Balance?

    suspend fun performOperation(): ExternalSignCommunicator.Response?

    suspend fun readableOperationContent(): String

    suspend fun shutdown() {}
}
