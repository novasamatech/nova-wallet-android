package io.novafoundation.nova.common.utils.formatting

import io.novafoundation.nova.common.resources.ResourceManager

interface Formatable {

    fun format(resourceManager: ResourceManager): String?
}
