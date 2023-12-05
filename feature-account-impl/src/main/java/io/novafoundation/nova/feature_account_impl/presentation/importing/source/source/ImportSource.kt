package io.novafoundation.nova.feature_account_impl.presentation.importing.source.source

import androidx.annotation.StringRes
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.feature_account_api.domain.model.AddAccountType
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.presentation.common.accountSource.AccountSource
import io.novafoundation.nova.feature_account_impl.presentation.importing.ImportAccountViewModel
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.view.ImportSourceView
import kotlinx.coroutines.flow.Flow

class ImportError(
    @StringRes val titleRes: Int = R.string.common_error_general_title,
    @StringRes val messageRes: Int = R.string.common_undefined_error_message
)

sealed class ImportSource(@StringRes titleRes: Int) : AccountSource(titleRes) {

    abstract val encryptionOptionsAvailable: Boolean

    abstract val fieldsValidFlow: Flow<Boolean>

    abstract fun initializeView(viewModel: ImportAccountViewModel, fragment: BaseFragment<*>): ImportSourceView<*>

    abstract suspend fun performImport(addAccountType: AddAccountType): Result<Unit>

    open fun handleError(throwable: Throwable): ImportError? = null
}
