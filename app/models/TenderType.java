package models;

import java.util.Map;
import java.util.HashMap;
import static java.util.Objects.requireNonNull;
import java.util.Arrays;

public enum TenderType {
    PAYPAL, ACCOUNTING_BILL;

    private static final TenderType byIndex[] = TenderType.class.getEnumConstants();
    private static final Map<String, TenderType> byString;
    static {
        byString = new HashMap<>();
        for (TenderType tt: byIndex) {
            byString.put(tt.toString(), tt);
        }
    }

    public static TenderType byIndex(int code) {
        return byIndex[code];
    }

    public static TenderType fromString(String s) {
        TenderType tt = byString.get(requireNonNull(s));
        if (tt == null) 
            throw new IllegalArgumentException
                ("'" + s + "' is invalid tender type. " +
                 "Available tender types are: " + Arrays.toString(byIndex));
        return tt;
    }

    public static TenderType[] all() {
        return byIndex.clone();
    }
}
