package io.novafoundation.nova.feature_account_impl.presentation.importing.source.view

import android.content.Context
import android.util.AttributeSet
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import io.novafoundation.nova.common.utils.EventObserver
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.observe
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_account_impl.databinding.ImportSourceJsonBinding
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.source.JsonImportSource

class JsonImportView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ImportSourceView<JsonImportSource>(context, attrs, defStyleAttr) {

    private val binder = ImportSourceJsonBinding.inflate(inflater(), this)

    override val nameInputViews: ImportAccountNameViews
        get() = ImportAccountNameViews(
            nameInput = binder.importJsonUsernameInput,
            visibilityDependent = emptyList()
        )

    override fun observeSource(source: JsonImportSource, lifecycleOwner: LifecycleOwner) {
        val scope = lifecycleOwner.lifecycle.coroutineScope

        source.jsonContentFlow.observe(scope, binder.importJsonContent::setMessage)

        binder.importJsonContent.setWholeClickListener { source.jsonClicked() }

        source.showJsonInputOptionsEvent.observe(
            lifecycleOwner,
            EventObserver {
                showJsonInputOptionsSheet(source)
            }
        )

        binder.importJsonPasswordInput.content.bindTo(source.passwordFlow, scope)

        binder.importJsonContent.setOnClickListener {
            source.jsonClicked()
        }

        source.showNetworkWarningFlow.observe(scope) {
            binder.importJsonNoNetworkInfo.setVisible(it)
        }
    }

    private fun showJsonInputOptionsSheet(source: JsonImportSource) {
        JsonPasteOptionsSheet(context, source::pasteClicked, source::chooseFileClicked)
            .show()
    }
}
