package jp.co.soramitsu.feature_account_impl.presentation.exporting.json.password

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import jp.co.soramitsu.common.base.BaseViewModel
import jp.co.soramitsu.common.utils.combine
import jp.co.soramitsu.common.utils.flowOf
import jp.co.soramitsu.common.utils.inBackground
import jp.co.soramitsu.common.utils.invoke
import jp.co.soramitsu.common.utils.lazyAsync
import jp.co.soramitsu.feature_account_api.data.mappers.mapChainToUi
import jp.co.soramitsu.feature_account_impl.domain.account.export.json.ExportJsonInteractor
import jp.co.soramitsu.feature_account_impl.presentation.AccountRouter
import jp.co.soramitsu.feature_account_impl.presentation.exporting.ExportPayload
import jp.co.soramitsu.feature_account_impl.presentation.exporting.json.confirm.ExportJsonConfirmPayload
import jp.co.soramitsu.runtime.multiNetwork.ChainRegistry
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
