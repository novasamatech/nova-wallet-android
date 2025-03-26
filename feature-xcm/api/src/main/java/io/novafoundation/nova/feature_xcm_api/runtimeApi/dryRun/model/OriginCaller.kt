package io.novafoundation.nova.feature_xcm_api.runtimeApi.dryRun.model

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.utils.scale.ToDynamicScaleInstance
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum

sealed class OriginCaller : ToDynamicScaleInstance {

    sealed class System : OriginCaller() {

        object Root : System() {

            override fun toEncodableInstance(): Any? {
                return wrapInSystemDict(DictEnum.Entry("Root", null))
            }
        }

        class Signed(val accountId: AccountIdKey) : System() {

            override fun toEncodableInstance(): Any? {
                return wrapInSystemDict(DictEnum.Entry("Signed", accountId.value))
            }
        }

        protected fun wrapInSystemDict(inner: Any): DictEnum.Entry<*> {
            return DictEnum.Entry("system", inner)
        }
    }
}
