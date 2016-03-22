package models;

public enum TransactionType {
    NORMAL, PAYPAL;

    private static final TransactionType byIndex[] = TransactionType.class.getEnumConstants();

    public static TransactionType byIndex(int index) {
        return byIndex[index];
    }
}
