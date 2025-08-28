package io.novafoundation.nova.hydra_dx_math.stableswap;

public class StableSwapMathBridge {

    static {
        System.loadLibrary("hydra_dx_math_java");
    }

    public static native String calculate_out_given_in(
        String reserves,
        int asset_in,
        int asset_out,
        String amount_in,
        String amplification,
        String fee,
        String pegs
    );

    public static native String calculate_in_given_out(
        String reserves,
        int asset_in,
        int asset_out,
        String amount_out,
        String amplification,
        String fee,
        String pegs
    );

    public static native String calculate_amplification(
        String initial_amplification,
        String final_amplification,
        String initial_block,
        String final_block,
        String current_block
    );

    public static native String calculate_shares(
        String reserves,
        String assets,
        String amplification,
        String share_issuance,
        String fee,
        String pegs
    );

    public static native String calculate_shares_for_amount(
        String reserves,
        int asset_in,
        String amount,
        String amplification,
        String share_issuance,
        String fee,
        String pegs
    );

    public static native String calculate_add_one_asset(
        String reserves,
        String shares,
        int asset_in,
        String amplification,
        String share_issuance,
        String fee,
        String pegs
    );

    public static native String calculate_liquidity_out_one_asset(
        String reserves,
        String shares,
        int asset_out,
        String amplification,
        String share_issuance,
        String withdraw_fee,
        String pegs
    );
}
