package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

enum class AmountWithdrawMode {

    SPECIFIED_EXACT {
        override fun totalTransferAmount(sendAmount: Balance, fees: Balance): Balance {
            return sendAmount
        }

    },

    ADD_FEES_ON_TOP {
        override fun totalTransferAmount(sendAmount: Balance, fees: Balance): Balance {
            return sendAmount + fees
        }

    };

    abstract fun totalTransferAmount(sendAmount: Balance, fees: Balance): Balance
}
