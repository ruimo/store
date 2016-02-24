package models;

public enum TransactionStatus {
    ORDERED, SHIPPED, CANCELED
    ,CONFIRMED // Shipping date and delivery date are recorded.
    ;

    private static final TransactionStatus byIndex[] = TransactionStatus.class.getEnumConstants();

    public static TransactionStatus byIndex(int index) {
        return byIndex[index];
    }
}
