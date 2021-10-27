package io.novafoundation.nova.feature_wallet_api.presentation.view

import android.content.Context
import android.util.AttributeSet
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.view.TableCellView
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeStatus

class FeeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : TableCellView(context, attrs, defStyle) {

    init {
        setTitle(R.string.network_fee)

        setFeeStatus(FeeStatus.Loading)
    }

    fun setFeeStatus(feeStatus: FeeStatus) {
        when (feeStatus) {
            is FeeStatus.Loading -> {
                showProgress()
            }
            is FeeStatus.Error -> {
                showValue(context.getString(R.string.common_error_general_title))
            }
            is FeeStatus.Loaded -> {
                showValue(
                    primary = feeStatus.feeModel.displayToken,
                    secondary = feeStatus.feeModel.displayFiat
                )
            }
        }
    }
}
