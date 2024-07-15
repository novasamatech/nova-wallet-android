package io.novafoundation.nova.hydra_dx_math.xyk;

public class HYKSwapMathBridge {

    static {
        System.loadLibrary("hydra_dx_math_java");
    }

    public static native String calculate_out_given_in(
        String balanceIn,
        String balanceOut,
        String amountIn
    );

    public static native String calculate_in_given_out(
        String balanceIn,
        String balanceOut,
        String amountOut
    );

    public static native String calculate_pool_trade_fee(
        String amount,
        String feeNumerator,
        String feeDenominator
    );
}
