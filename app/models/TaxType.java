package models;

public enum TaxType {
    OUTER_TAX, INNER_TAX, NON_TAX;

    private static final TaxType byIndex[] = TaxType.class.getEnumConstants();

    public static TaxType byIndex(int index) {
        return byIndex[index];
    }
}
