package models;

public enum PaypalStatus {
    START, PREPARED, COMPLETED, CANCELED, ERROR;

    private static final PaypalStatus byIndex[] = PaypalStatus.class.getEnumConstants();

    public static PaypalStatus byIndex(int index) {
        return byIndex[index];
    }
}
