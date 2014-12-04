package models;

public enum SiteItemTextMetadataType {
    PRICE_MEMO, LIST_PRICE_MEMO;

    private static final SiteItemTextMetadataType byIndex[] = 
      SiteItemTextMetadataType.class.getEnumConstants();

    public static SiteItemTextMetadataType byIndex(int index) {
        return byIndex[index];
    }

    public static SiteItemTextMetadataType[] all() {
        return SiteItemTextMetadataType.class.getEnumConstants();
    }
}
