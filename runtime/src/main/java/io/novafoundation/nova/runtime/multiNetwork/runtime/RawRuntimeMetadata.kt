package io.novafoundation.nova.runtime.multiNetwork.runtime

class RawRuntimeMetadata(
    val metadataContent: ByteArray,

    /**
     * Whether metadata stored is opaque form
     *
     * Opaque form of metadata is equivalent to Option<Vec<u8>>
     * So the layout for opaque metadata will have a form
     * 1 byte (`Optional` flag) + 2..4 bytes (CompactInt, length of Vec) + regular metadata (content of Vec)
     */
    val isOpaque: Boolean
)
