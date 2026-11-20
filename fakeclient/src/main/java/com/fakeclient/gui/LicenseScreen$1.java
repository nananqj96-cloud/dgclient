/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fakeclient.license.License$Status
 */
package com.fakeclient.gui;
import com.fakeclient.license.License$Status;

import com.fakeclient.license.License;

public class LicenseScreen$1 {
    static final /* synthetic */ int[] $SwitchMap$com$fakeclient$license$License$Status;

    static {
        $SwitchMap$com$fakeclient$license$License$Status = new int[License$Status.values().length];
        try {
            LicenseScreen$1.$SwitchMap$com$fakeclient$license$License$Status[License$Status.VALID.ordinal()] = 1;
        }
        catch (NoSuchFieldError noSuchFieldError) {
            // empty catch block
        }
        try {
            LicenseScreen$1.$SwitchMap$com$fakeclient$license$License$Status[License$Status.CHECKING.ordinal()] = 2;
        }
        catch (NoSuchFieldError noSuchFieldError) {
            // empty catch block
        }
        try {
            LicenseScreen$1.$SwitchMap$com$fakeclient$license$License$Status[License$Status.UNCHECKED.ordinal()] = 3;
        }
        catch (NoSuchFieldError noSuchFieldError) {
            // empty catch block
        }
        try {
            LicenseScreen$1.$SwitchMap$com$fakeclient$license$License$Status[License$Status.ERROR.ordinal()] = 4;
        }
        catch (NoSuchFieldError noSuchFieldError) {
            // empty catch block
        }
    }
}
