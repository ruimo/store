package models;

public enum TransactionType {
    NORMAL;

    private static final TransactionType byIndex[] = TransactionType.class.getEnumConstants();

    public static TransactionType byIndex(int index) {
        return byIndex[index];
    }
}
