package models;

import java.util.Arrays;

public enum Sex {
    MALE, FEMALE;

    private static final Sex byIndex[] = Sex.class.getEnumConstants();

    public static Sex byIndex(int code) {
        return byIndex[code];
    }

    public static Sex[] all() {
        return Arrays.copyOfRange(byIndex, 0, byIndex.length);
    }
}
