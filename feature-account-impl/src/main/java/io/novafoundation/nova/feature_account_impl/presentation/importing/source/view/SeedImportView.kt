package io.novafoundation.nova.feature_account_impl.presentation.importing.source.view

import android.content.Context
import android.util.AttributeSet
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.view.shape.getIdleDrawable
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.source.RawSeedImportSource
import kotlinx.android.synthetic.main.import_source_seed.view.importSeedContent
import kotlinx.android.synthetic.main.import_source_seed.view.importSeedContentContainer
import kotlinx.android.synthetic.main.import_source_seed.view.importSeedUsernameHint
import kotlinx.android.synthetic.main.import_source_seed.view.importSeedUsernameInput

class SeedImportView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ImportSourceView<RawSeedImportSource>(R.layout.import_source_seed, context, attrs, defStyleAttr) {

    override val nameInputViews: ImportAccountNameViews
        get() = ImportAccountNameViews(
            nameInput = importSeedUsernameInput,
            visibilityDependent = listOf(importSeedUsernameHint)
        )

    init {
        importSeedContentContainer.background = context.getIdleDrawable()
    }

    override fun observeSource(source: RawSeedImportSource, lifecycleOwner: LifecycleOwner) {
        importSeedContent.bindTo(source.rawSeedFlow, lifecycleOwner.lifecycle.coroutineScope)
    }
}
