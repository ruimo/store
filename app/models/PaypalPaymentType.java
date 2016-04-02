package models;

public enum PaypalPaymentType {
    EXPRESS_CHECKOUT, WEB_PAYMENT_PLUS;

    private static final PaypalPaymentType byIndex[] = PaypalPaymentType.class.getEnumConstants();

    public static PaypalPaymentType byIndex(int index) {
        return byIndex[index];
    }
}

    
