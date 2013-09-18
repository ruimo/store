package models;

public enum SiteItemNumericMetadataType {
    STOCK;

    private static final SiteItemNumericMetadataType byIndex[] = 
      SiteItemNumericMetadataType.class.getEnumConstants();

    public static SiteItemNumericMetadataType byIndex(int index) {
        return byIndex[index];
    }
}
