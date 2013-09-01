package models;

public enum MetadataType {
    STOCK, HEIGHT;

    private static final MetadataType byIndex[] = MetadataType.class.getEnumConstants();

    public static MetadataType byIndex(int index) {
        return byIndex[index];
    }
}
