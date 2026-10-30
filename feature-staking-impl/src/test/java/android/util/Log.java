package android.util;

/**
 * JVM-only diagnostic sink for the real ComputationalCache used in repository tests.
 * Other Android API calls remain unsupported in local tests.
 */
public final class Log {

    private Log() {}

    public static int d(String tag, String message) {
        return 0;
    }
}
