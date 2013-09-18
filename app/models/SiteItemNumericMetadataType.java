package models;

public enum SiteItemNumericMetadataType {
    STOCK, PROMOTION;

    private static final SiteItemNumericMetadataType byIndex[] = 
      SiteItemNumericMetadataType.class.getEnumConstants();

    public static SiteItemNumericMetadataType byIndex(int index) {
        return byIndex[index];
    }
}
