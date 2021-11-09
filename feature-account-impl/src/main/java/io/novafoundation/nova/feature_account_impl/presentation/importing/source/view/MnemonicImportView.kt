package io.novafoundation.nova.feature_account_impl.presentation.importing.source.view

import android.content.Context
import android.util.AttributeSet
import androidx.lifecycle.LifecycleOwner
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.nameInputFilters
import io.novafoundation.nova.common.view.shape.getIdleDrawable
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.model.ImportSource
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.model.MnemonicImportSource
import kotlinx.android.synthetic.main.import_source_mnemonic.view.importMnemnonicUsernameHint
import kotlinx.android.synthetic.main.import_source_mnemonic.view.importMnemonicContent
import kotlinx.android.synthetic.main.import_source_mnemonic.view.importMnemonicContentContainer
import kotlinx.android.synthetic.main.import_source_mnemonic.view.importMnemonicUsernameInput

class MnemonicImportView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ImportSourceView(R.layout.import_source_mnemonic, context, attrs, defStyleAttr) {

    override val nameInputViews: ImportAccountNameViews
        get() = ImportAccountNameViews(
            nameInput = importMnemonicUsernameInput,
            visibilityDependent = listOf(importMnemnonicUsernameHint)
        )

    init {
        importMnemonicContentContainer.background = context.getIdleDrawable()

        importMnemonicUsernameInput.content.filters = nameInputFilters()
    }

    override fun observeSource(source: ImportSource, lifecycleOwner: LifecycleOwner) {
        require(source is MnemonicImportSource)

        importMnemonicContent.bindTo(source.mnemonicContentLiveData, lifecycleOwner)
    }
}
