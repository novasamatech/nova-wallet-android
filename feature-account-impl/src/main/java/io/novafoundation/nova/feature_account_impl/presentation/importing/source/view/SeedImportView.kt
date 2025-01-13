package io.novafoundation.nova.feature_account_impl.presentation.importing.source.view

import android.content.Context
import android.util.AttributeSet
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.view.shape.getIdleDrawable
import io.novafoundation.nova.feature_account_impl.databinding.ImportSourceSeedBinding
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.source.RawSeedImportSource

class SeedImportView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ImportSourceView<RawSeedImportSource>(context, attrs, defStyleAttr) {

    private val binder = ImportSourceSeedBinding.inflate(inflater(), this)

    override val nameInputViews: ImportAccountNameViews
        get() = ImportAccountNameViews(
            nameInput = binder.importSeedUsernameInput,
            visibilityDependent = listOf(binder.importSeedUsernameHint)
        )

    init {
        binder.importSeedContentContainer.background = context.getIdleDrawable()
    }

    override fun observeSource(source: RawSeedImportSource, lifecycleOwner: LifecycleOwner) {
        binder.importSeedContent.bindTo(source.rawSeedFlow, lifecycleOwner.lifecycle.coroutineScope)
    }
}
