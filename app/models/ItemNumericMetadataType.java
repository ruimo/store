package models;

public enum ItemNumericMetadataType {
    HEIGHT;

    private static final ItemNumericMetadataType byIndex[] = ItemNumericMetadataType.class.getEnumConstants();

    public static ItemNumericMetadataType byIndex(int index) {
        return byIndex[index];
    }
}
