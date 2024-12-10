package io.novafoundation.nova.feature_account_impl.presentation.importing.source.view

import android.content.Context
import android.util.AttributeSet
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.view.shape.getIdleDrawable
import io.novafoundation.nova.feature_account_impl.databinding.ImportSourceMnemonicBinding
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.source.MnemonicImportSource

class MnemonicImportView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ImportSourceView<MnemonicImportSource>(context, attrs, defStyleAttr) {

    private val binder = ImportSourceMnemonicBinding.inflate(inflater(), this)

    override val nameInputViews: ImportAccountNameViews
        get() = ImportAccountNameViews(
            nameInput = binder.importMnemonicUsernameInput,
            visibilityDependent = listOf(binder.importMnemnonicUsernameHint)
        )

    init {
        binder.importMnemonicContentContainer.background = context.getIdleDrawable()
    }

    override fun observeSource(source: MnemonicImportSource, lifecycleOwner: LifecycleOwner) {
        binder.importMnemonicContent.bindTo(source.mnemonicContentFlow, lifecycleOwner.lifecycle.coroutineScope)
    }
}
