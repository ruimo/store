package models;

public enum SiteItemNumericMetadataType {
    STOCK, PROMOTION, SHIPPING_SIZE, HIDE;

    private static final SiteItemNumericMetadataType byIndex[] = 
      SiteItemNumericMetadataType.class.getEnumConstants();

    public static SiteItemNumericMetadataType byIndex(int index) {
        return byIndex[index];
    }

    public static SiteItemNumericMetadataType[] all() {
        return SiteItemNumericMetadataType.class.getEnumConstants();
    }
}
