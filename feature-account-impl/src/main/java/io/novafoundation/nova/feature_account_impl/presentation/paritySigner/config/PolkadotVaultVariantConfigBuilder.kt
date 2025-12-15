package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.config

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.config.PolkadotVaultVariantConfig
import io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.config.PolkadotVaultVariantConfig.Common
import io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.config.PolkadotVaultVariantConfig.ConnectPage
import io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.config.PolkadotVaultVariantConfig.ConnectPage.Instruction
import io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.config.PolkadotVaultVariantConfig.Sign

@DslMarker
annotation class VariantConfigBuilderDsl

typealias DslBuilding<T> = T.() -> Unit

@VariantConfigBuilderDsl
interface PolkadotVaultVariantConfigBuilder {

    fun connectPage(builder: DslBuilding<ConnectPageBuilder>)

    fun sign(builder: DslBuilding<SignBuilder>)

    fun common(builder: DslBuilding<CommonBuilder>)

    @VariantConfigBuilderDsl
    interface ConnectPageBuilder {

        fun name(name: String)

        fun instructions(builder: DslBuilding<InstructionsBuilder>)

        @VariantConfigBuilderDsl
        interface InstructionsBuilder {

            fun step(@StringRes contentRes: Int)

            fun step(content: CharSequence)

            fun image(@StringRes labelRes: Int?, @DrawableRes imageRes: Int)
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

internal fun BuildPolkadotVaultVariantConfig(
    resourceManager: ResourceManager,
    builder: DslBuilding<PolkadotVaultVariantConfigBuilder>
): PolkadotVaultVariantConfig {
    return RealPolkadotVaultVariantConfigBuilder(resourceManager).apply(builder).build()
}

private class RealPolkadotVaultVariantConfigBuilder(
    private val resourceManager: ResourceManager
) : PolkadotVaultVariantConfigBuilder {

    private val pages = mutableListOf<ConnectPage>()

    private var sign: Sign? = null
    private var common: Common? = null

    override fun connectPage(builder: DslBuilding<PolkadotVaultVariantConfigBuilder.ConnectPageBuilder>) {
        val page = RealConnectPageBuilder(resourceManager).apply(builder).build()
        pages += page
    }

    override fun sign(builder: DslBuilding<PolkadotVaultVariantConfigBuilder.SignBuilder>) {
        sign = RealSignBuilder().apply(builder).build()
    }

    override fun common(builder: DslBuilding<PolkadotVaultVariantConfigBuilder.CommonBuilder>) {
        common = RealCommonBuilder().apply(builder).build()
    }

    fun build(): PolkadotVaultVariantConfig {
        require(pages.isNotEmpty()) { "At least one connectPage { } must be defined" }

        val sign = requireNotNull(sign) { "sign { } block is required" }
        val common = requireNotNull(common) { "common { } block is required" }

        return PolkadotVaultVariantConfig(pages, sign, common)
    }
}

private class RealConnectPageBuilder(
    private val resourceManager: ResourceManager
) : PolkadotVaultVariantConfigBuilder.ConnectPageBuilder {

    private var name: String? = null
    private var instructions: List<Instruction>? = null

    override fun name(name: String) {
        this.name = name
    }

    override fun instructions(builder: DslBuilding<PolkadotVaultVariantConfigBuilder.ConnectPageBuilder.InstructionsBuilder>) {
        instructions = RealInstructionsBuilder(resourceManager).apply(builder).build()
    }

    fun build(): ConnectPage {
        val name = requireNotNull(name) { "name must be provided for each connectPage { }" }
        val instructions = requireNotNull(instructions) { "instructions { } must be provided for each connectPage { }" }
        return ConnectPage(name, instructions)
    }
}

private class RealInstructionsBuilder(
    private val resourceManager: ResourceManager
) : PolkadotVaultVariantConfigBuilder.ConnectPageBuilder.InstructionsBuilder {

    private var stepsCounter = 0
    private val instructions = mutableListOf<Instruction>()

    override fun step(contentRes: Int) {
        val content = resourceManager.getText(contentRes)
        step(content)
    }

    override fun step(content: CharSequence) {
        stepsCounter += 1

        val stepInstruction = Instruction.Step(stepsCounter, content)
        instructions.add(stepInstruction)
    }

    override fun image(labelRes: Int?, imageRes: Int) {
        val imageInstruction = Instruction.Image(labelRes?.let { resourceManager.getString(it) }, imageRes)
        instructions.add(imageInstruction)
    }

    fun build(): List<Instruction> {
        require(instructions.isNotEmpty()) { "instructions { } must not be empty" }
        return instructions
    }
}

private class RealSignBuilder : PolkadotVaultVariantConfigBuilder.SignBuilder {

    private var _troubleShootingLink: String? = null
    override var troubleShootingLink: String
        get() = requireNotNull(_troubleShootingLink) { "troubleShootingLink must be set" }
        set(value) {
            _troubleShootingLink = value
        }

    private var _supportsProofSigning: Boolean? = null
    override var supportsProofSigning: Boolean
        get() = requireNotNull(_supportsProofSigning) { "supportsProofSigning must be set" }
        set(value) {
            _supportsProofSigning = value
        }

    fun build(): Sign {
        return Sign(troubleShootingLink, supportsProofSigning)
    }
}

private class RealCommonBuilder : PolkadotVaultVariantConfigBuilder.CommonBuilder {

    private var _iconRes: Int? = null
    private var _nameRes: Int? = null

    override var iconRes: Int
        get() = requireNotNull(_iconRes) { "iconRes must be set" }
        set(@DrawableRes value) {
            _iconRes = value
        }

    override var nameRes: Int
        get() = requireNotNull(_nameRes) { "nameRes must be set" }
        set(@StringRes value) {
            _nameRes = value
        }

    fun build(): Common {
        return Common(iconRes = iconRes, nameRes = nameRes)
    }
}
