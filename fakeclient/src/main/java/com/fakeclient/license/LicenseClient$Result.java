package com.fakeclient.license;

/**
 * Result of a license server check.
 * Cracked: all accessors report a valid, reached result.
 */
public record LicenseClient$Result(boolean valid, boolean reached, String code, String message, long expiry) {

    @Override
    public boolean valid() {
        return true;
    }

    @Override
    public boolean reached() {
        return true;
    }

    @Override
    public String code() {
        return "";
    }

    @Override
    public String message() {
        return "";
    }

    @Override
    public long expiry() {
        return Long.MAX_VALUE;
    }
}
