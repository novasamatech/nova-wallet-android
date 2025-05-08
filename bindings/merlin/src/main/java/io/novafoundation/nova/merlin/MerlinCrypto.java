package io.novafoundation.nova.merlin;

public class MerlinCrypto {

    static {
        System.loadLibrary("merlin_crypto_java");
    }

    public static native byte[] generateTranscript(
        byte[] publicKey,
        byte[] privateKey,
        byte[] message
    );
}
