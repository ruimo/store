package models;

public enum ItemTextMetadataType {
    ABOUT_HEIGHT;

    private static final ItemTextMetadataType byIndex[] = ItemTextMetadataType.class.getEnumConstants();

    public static ItemTextMetadataType byIndex(int index) {
        return byIndex[index];
    }

    public static ItemTextMetadataType[] all() {
        return ItemTextMetadataType.class.getEnumConstants();
    }
}
