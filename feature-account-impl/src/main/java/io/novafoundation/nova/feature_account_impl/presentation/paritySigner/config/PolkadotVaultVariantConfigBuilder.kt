package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.config

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.config.PolkadotVaultVariantConfig
import io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.config.PolkadotVaultVariantConfig.Common
import io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.config.PolkadotVaultVariantConfig.Connect
import io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.config.PolkadotVaultVariantConfig.Connect.Instruction
import io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.config.PolkadotVaultVariantConfig.Sign
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.config.PolkadotVaultVariantConfigBuilder.CommonBuilder
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.config.PolkadotVaultVariantConfigBuilder.ConnectBuilder
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.config.PolkadotVaultVariantConfigBuilder.ConnectBuilder.InstructionsBuilder
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.config.PolkadotVaultVariantConfigBuilder.SignBuilder

@VariantConfigBuilderDsl
interface PolkadotVaultVariantConfigBuilder {

    fun connect(builder: DslBuilding<ConnectBuilder>)

    fun sign(builder: DslBuilding<SignBuilder>)

    fun common(builder: DslBuilding<CommonBuilder>)

    @VariantConfigBuilderDsl
    interface ConnectBuilder {

        fun instructions(builder: DslBuilding<InstructionsBuilder>)

        @VariantConfigBuilderDsl
        interface InstructionsBuilder {

            fun step(@StringRes contentRes: Int)

            fun step(content: CharSequence)

            fun image(@StringRes labelRes: Int, @DrawableRes imageRes: Int)
        }
    }

    interface SignBuilder {

        var troubleShootingLink: String

        var supportsProofSigning: Boolean
    }

    interface CommonBuilder {

        @get:DrawableRes
        var iconRes: Int

        @get:StringRes
        var nameRes: Int
    }
}

@DslMarker
annotation class VariantConfigBuilderDsl

typealias DslBuilding<T> = T.() -> Unit

internal fun BuildPolkadotVaultVariantConfig(
    resourceManager: ResourceManager,
    builder: DslBuilding<PolkadotVaultVariantConfigBuilder>
): PolkadotVaultVariantConfig {
    return RealPolkadotVaultVariantConfigBuilder(resourceManager).apply(builder).build()
}

private class RealPolkadotVaultVariantConfigBuilder(
    private val resourceManager: ResourceManager
) : PolkadotVaultVariantConfigBuilder {

    private var connect: Connect? = null
    private var sign: Sign? = null
    private var common: Common? = null

    override fun connect(builder: DslBuilding<ConnectBuilder>) {
        connect = RealConnectBuilder(resourceManager).apply(builder).build()
    }

    override fun sign(builder: DslBuilding<SignBuilder>) {
        sign = RealSignBuilder().apply(builder).build()
    }

    override fun common(builder: DslBuilding<CommonBuilder>) {
        common = RealCommonBuilder().apply(builder).build()
    }

    fun build(): PolkadotVaultVariantConfig {
        val connect = requireNotNull(connect)
        val sign = requireNotNull(sign)
        val common = requireNotNull(common)

        return PolkadotVaultVariantConfig(connect, sign, common)
    }
}

private class RealConnectBuilder(
    private val resourceManager: ResourceManager
) : ConnectBuilder {

    private var instructions: List<Instruction>? = null

    override fun instructions(builder: DslBuilding<InstructionsBuilder>) {
        instructions = RealInstructionsBuilder(resourceManager).apply(builder).build()
    }

    fun build(): Connect {
        val instructions = requireNotNull(instructions)

        return Connect(instructions)
    }
}

private class RealInstructionsBuilder(
    private val resourceManager: ResourceManager
) : InstructionsBuilder {

    private var stepsCounter = 0
    private var instructions = mutableListOf<Instruction>()

    override fun step(contentRes: Int) {
        val content = resourceManager.getText(contentRes)
        step(content)
    }

    override fun step(content: CharSequence) {
        stepsCounter += 1

        val stepInstruction = Instruction.Step(stepsCounter, content)
        instructions.add(stepInstruction)
    }

    override fun image(labelRes: Int, imageRes: Int) {
        val imageInstruction = Instruction.Image(resourceManager.getString(labelRes), imageRes)
        instructions.add(imageInstruction)
    }

    fun build(): List<Instruction> {
        require(instructions.isNotEmpty())

        return instructions
    }
}

private class RealSignBuilder : SignBuilder {

    private var _troubleShootingLink: String? = null
    override var troubleShootingLink: String
        get() = requireNotNull(_troubleShootingLink)
        set(value) {
            _troubleShootingLink = value
        }

    private var _supportsProofSigning: Boolean? = null
    override var supportsProofSigning: Boolean
        get() = requireNotNull(_supportsProofSigning)
        set(value) {
            _supportsProofSigning = value
        }

    fun build(): Sign {
        return Sign(troubleShootingLink, supportsProofSigning)
    }
}

private class RealCommonBuilder : CommonBuilder {

    private var _iconRes: Int? = null
    private var _nameRes: Int? = null

    override var iconRes: Int
        get() = requireNotNull(_iconRes)
        set(@DrawableRes value) {
            _iconRes = value
        }

    override var nameRes: Int
        get() = requireNotNull(_nameRes)
        set(@StringRes value) {
            _nameRes = value
        }

    fun build(): Common {
        return Common(iconRes = iconRes, nameRes = nameRes)
    }
}
