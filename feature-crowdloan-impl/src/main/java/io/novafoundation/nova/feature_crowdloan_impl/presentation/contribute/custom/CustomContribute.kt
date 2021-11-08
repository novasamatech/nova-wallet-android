package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

sealed class ApplyActionState {
    object Available : ApplyActionState()

    class Unavailable(val reason: String) : ApplyActionState()
}

interface CustomContributeViewState {

    suspend fun generatePayload(): Result<BonusPayload>

    val applyActionState: Flow<ApplyActionState>
}

abstract class CustomContributeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle) {

    abstract fun bind(viewState: CustomContributeViewState, scope: LifecycleCoroutineScope)
}

interface BonusPayload : Parcelable {

    fun bonusText(amount: BigDecimal): String
}
