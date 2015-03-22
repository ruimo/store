package models;

import java.util.Map;
import java.util.HashMap;

// Used for inquiry_type column in item_inquiry table.
enum ItemInquiryType {
    QUERY, RESERVATION;

    private static final ItemInquiryType byIndex[] = ItemInquiryType.class.getEnumConstants();
    private static final Map<String, ItemInquiryType> byName = new HashMap<>();

    static {
        for (ItemInquiryType cc: byIndex) {
            byName.put(cc.toString(), cc);
        }
    }

    public static ItemInquiryType byIndex(int index) {
        return byIndex[index];
    }

    public static ItemInquiryType byName(String name) {
        ItemInquiryType t = byName.get(name);
        if (t == null) throw new IllegalArgumentException("Name '" + name + "' not found.");
        return t;
    }

    public int code() {
        return ordinal();
    }
}
