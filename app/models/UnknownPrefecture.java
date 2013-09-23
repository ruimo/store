package models;

public enum UnknownPrefecture implements Prefecture {
    UNKNOWN;

    private static final UnknownPrefecture byIndex[] = UnknownPrefecture.class.getEnumConstants();

    public static UnknownPrefecture byIndex(int code) {
        return byIndex[code];
    }

    @Override public int code() {
        return ordinal();
    }

    @Override public boolean isUnknown() {
        return this == UNKNOWN;
    }
}
