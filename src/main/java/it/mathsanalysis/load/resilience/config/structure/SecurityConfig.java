package it.mathsanalysis.load.resilience.config.structure;

public record SecurityConfig(
        boolean encryptionEnabled,
        boolean auditLogging,
        boolean sensitiveDataMasking
) {
    public static SecurityConfig.Builder builder() {
        return new SecurityConfig.Builder();
    }

    public static SecurityConfig defaults() {
        return builder().build();
    }

    public static class Builder {
        private boolean encryptionEnabled = false;
        private boolean auditLogging = false;
        private boolean sensitiveDataMasking = true;

        public SecurityConfig.Builder encryptionEnabled(boolean encryptionEnabled) {
            this.encryptionEnabled = encryptionEnabled;
            return this;
        }

        public SecurityConfig.Builder auditLogging(boolean auditLogging) {
            this.auditLogging = auditLogging;
            return this;
        }

        public SecurityConfig.Builder sensitiveDataMasking(boolean sensitiveDataMasking) {
            this.sensitiveDataMasking = sensitiveDataMasking;
            return this;
        }

        public SecurityConfig build() {
            return new SecurityConfig(encryptionEnabled, auditLogging, sensitiveDataMasking);
        }
    }
}
