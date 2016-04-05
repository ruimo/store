package models;

public enum TransactionTypeCode {
    ACCOUNTING_BILL, PAYPAL_EXPRESS_CHECKOUT, PAYPAL_WEB_PAYMENT_PLUS;

    private static final TransactionTypeCode byIndex[] = TransactionTypeCode.class.getEnumConstants();

    public static TransactionTypeCode byIndex(int index) {
        return byIndex[index];
    }
}
