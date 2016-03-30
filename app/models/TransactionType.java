package models;

public enum TransactionType {
    NORMAL, PAYPAL, PAYPAL_WEB_PAYMENT_PLUS;

    private static final TransactionType byIndex[] = TransactionType.class.getEnumConstants();

    public static TransactionType byIndex(int index) {
        return byIndex[index];
    }
}
