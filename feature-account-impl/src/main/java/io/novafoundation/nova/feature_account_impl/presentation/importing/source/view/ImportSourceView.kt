package io.novafoundation.nova.feature_account_impl.presentation.importing.source.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.annotation.LayoutRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LifecycleOwner
import io.novafoundation.nova.common.view.InputField
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.impl.setupAccountNameChooserUi
import io.novafoundation.nova.feature_account_impl.presentation.importing.ImportAccountViewModel
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.model.ImportSource

class ImportAccountNameViews(
    val nameInput: InputField,
    val visibilityDependent: List<View>,
)

abstract class ImportSourceView @JvmOverloads constructor(
    @LayoutRes layoutId: Int,
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {

    protected abstract val nameInputViews: ImportAccountNameViews

    init {
        View.inflate(context, layoutId, this)
    }

    abstract fun observeSource(source: ImportSource, lifecycleOwner: LifecycleOwner)

    fun observeCommon(viewModel: ImportAccountViewModel, lifecycleOwner: LifecycleOwner) {
        setupAccountNameChooserUi(viewModel, nameInputViews.nameInput, lifecycleOwner, nameInputViews.visibilityDependent)
    }
}
