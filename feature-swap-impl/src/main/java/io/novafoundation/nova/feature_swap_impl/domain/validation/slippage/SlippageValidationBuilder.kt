package io.novafoundation.nova.feature_swap_impl.domain.validation.slippage

import io.novafoundation.nova.common.utils.Percent
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_swap_impl.domain.slippage.SlippageRepository

typealias SlippageValidationSystem = ValidationSystem<SlippageValidationPayload, SlippageValidationFailure>
typealias SlippageValidationSystemBuilder = ValidationSystemBuilder<SlippageValidationPayload, SlippageValidationFailure>

sealed class SlippageValidationFailure {

    class NotInAvailableRange(val minSlippage: Percent, val maxSlippage: Percent) : SlippageValidationFailure()

    object TooSmall : SlippageValidationFailure()

    object TooBig : SlippageValidationFailure()
}

class SlippageValidationPayload(val slippage: Percent)

fun SlippageValidationSystemBuilder.validateSlippage(
    slippageRepository: SlippageRepository
) = validate(
    SlippageValidation(
        minSlippage = slippageRepository.minSlippage(),
        maxSlippage = slippageRepository.maxSlippage(),
        smallSlippage = slippageRepository.smallSlippage(),
        bigSlippage = slippageRepository.bigSlippage()
    )
)

fun getSlippageValidationSystem(slippageRepository: SlippageRepository) = ValidationSystem {
    validateSlippage(slippageRepository)
}
