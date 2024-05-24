package io.novafoundation.nova.metadata_shortener;

public class MetadataShortener {

    static {
        System.loadLibrary("metadata_shortener_java");
    }

    public static native byte[] generate_extrinsic_proof(
        byte[] call,
        byte[] signed_extras,
        byte[] additional_signed,
        byte[] metadata,

        int spec_version,
        String spec_name,
        int base58_prefix,
        int decimals,
        String token_symbol
    );

    public static native byte[] generate_metadata_digest(
        byte[] metadata,
        int spec_version,
        String spec_name,
        int base58_prefix,
        int decimals,
        String token_symbol
    );
}
