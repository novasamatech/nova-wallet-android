package io.novafoundation.nova.hydra_dx_math.omnipool;

public class OmniPoolMathBridge {

    static {
        System.loadLibrary("hydra_dx_math_java");
    }

    public static native String calculate_out_given_in(
        String assetInReserve,
        String assetInHubReserve,
        String assetInShares,
        String assetOutReserve,
        String assetOutHubReserve,
        String assetOutShares,
        String amountIn,
        String assetFee,
        String protocolFee,
        String maxSlipFee
    );

    public static native String calculate_in_given_out(
        String assetInReserve,
        String assetInHubReserve,
        String assetInShares,
        String assetOutReserve,
        String assetOutHubReserve,
        String assetOutShares,
        String amountOut,
        String assetFee,
        String protocolFee,
        String maxSlipFee
    );
}
