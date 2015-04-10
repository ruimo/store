package models;

public enum SiteItemNumericMetadataType {
    STOCK
    ,PROMOTION
    ,SHIPPING_SIZE
    ,HIDE
    ,ITEM_DETAIL_TEMPLATE
    ,COUPON_TEMPLATE
    ,RESERVATION_ITEM // Show reserve button instead of put into cart button.
    ,INSTANT_COUPON
    ;

    private static final SiteItemNumericMetadataType byIndex[] = 
      SiteItemNumericMetadataType.class.getEnumConstants();

    public static SiteItemNumericMetadataType byIndex(int index) {
        return byIndex[index];
    }

    public static SiteItemNumericMetadataType[] all() {
        return SiteItemNumericMetadataType.class.getEnumConstants();
    }
}
