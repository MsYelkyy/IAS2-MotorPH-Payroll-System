package motorph.infrastructure.config;

/**
 * Stores email preview settings for QA testing.
 */
public class EmailPreviewConfig {

    private static boolean previewEnabled = true;

    // getters/setters
    public static boolean isPreviewEnabled() {
        return previewEnabled;
    }

    public static void setPreviewEnabled(boolean enabled) {
        previewEnabled = enabled;
    }
}