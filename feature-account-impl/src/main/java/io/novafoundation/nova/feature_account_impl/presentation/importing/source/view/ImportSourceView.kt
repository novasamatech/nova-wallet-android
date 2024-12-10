package io.novafoundation.nova.feature_account_impl.presentation.importing.source.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.lifecycle.LifecycleOwner
import io.novafoundation.nova.common.view.InputField
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.impl.setupAccountNameChooserUi
import io.novafoundation.nova.feature_account_impl.presentation.importing.ImportAccountViewModel
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.source.ImportSource

class ImportAccountNameViews(
    val nameInput: InputField,
    val visibilityDependent: List<View>,
)

abstract class ImportSourceView<S : ImportSource> @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    protected abstract val nameInputViews: ImportAccountNameViews

    init {
        orientation = VERTICAL
    }

    abstract fun observeSource(source: S, lifecycleOwner: LifecycleOwner)

    fun observeCommon(viewModel: ImportAccountViewModel, lifecycleOwner: LifecycleOwner) {
        setupAccountNameChooserUi(viewModel, nameInputViews.nameInput, lifecycleOwner, nameInputViews.visibilityDependent)
    }
}
