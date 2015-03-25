package models;

public enum ItemInquiryStatus implements ItemInquiryStatusType {
    SUBMITTED, DISMISSED, IN_PROGRESS, COMPLETED,
    FOR_EXTENSION_0, // Do not use this. For future extension.
    FOR_EXTENSION_1, // Do not use this. For future extension.
    FOR_EXTENSION_2, // Do not use this. For future extension.
    FOR_EXTENSION_3, // Do not use this. For future extension.
    FOR_EXTENSION_4, // Do not use this. For future extension.
    FOR_EXTENSION_5, // Do not use this. For future extension.
    FOR_EXTENSION_6, // Do not use this. For future extension.
    FOR_EXTENSION_7, // Do not use this. For future extension.
    FOR_EXTENSION_8, // Do not use this. For future extension.
    FOR_EXTENSION_9, // Do not use this. For future extension.
    FOR_USER_START; // You can freely use ordinal starts with this.

    private static final ItemInquiryStatus byIndex[] = ItemInquiryStatus.class.getEnumConstants();

    public static ItemInquiryStatus byIndex(int index) {
        return byIndex[index];
    }
}
