package io.novafoundation.nova.feature_account_impl.presentation.exporting.json.password

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.combine
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_impl.domain.account.export.json.ExportJsonInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.exporting.ExportPayload
import io.novafoundation.nova.feature_account_impl.presentation.exporting.json.confirm.ExportJsonConfirmPayload
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.launch

class ExportJsonPasswordViewModel(
    private val router: AccountRouter,
    private val interactor: ExportJsonInteractor,
    private val chainRegistry: ChainRegistry,
    private val payload: ExportPayload,
) : BaseViewModel() {

    private val chain by lazyAsync { chainRegistry.getChain(payload.chainId) }

    val passwordLiveData = MutableLiveData<String>()
    val passwordConfirmationLiveData = MutableLiveData<String>()

    val chainFlow = flowOf { mapChainToUi(chain()) }
        .inBackground()
        .share()

    val showDoNotMatchingErrorLiveData = passwordLiveData.combine(passwordConfirmationLiveData) { password, confirmation ->
        confirmation.isNotBlank() && confirmation != password
    }

    val nextEnabled = passwordLiveData.combine(passwordConfirmationLiveData, initial = false) { password, confirmation ->
        password.isNotBlank() && confirmation.isNotBlank() && password == confirmation
    }

    fun back() {
        router.back()
    }

    fun nextClicked() {
        val password = passwordLiveData.value!!

        viewModelScope.launch {
            interactor.generateRestoreJson(payload.metaId, payload.chainId, password)
                .onSuccess {
                    val confirmPayload = ExportJsonConfirmPayload(payload, it)

                    router.openExportJsonConfirm(confirmPayload)
                }.onFailure { it.message?.let(::showError) }
        }
    }
}
