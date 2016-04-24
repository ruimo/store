package models;

public enum UserRole {
    ADMIN, NORMAL, ANONYMOUS, ENTRY_USER;

    private static final UserRole byIndex[] = UserRole.class.getEnumConstants();

    public static UserRole byIndex(int index) {
        return byIndex[index];
    }

    public static UserRole[] all() {
        return byIndex.clone();
    }
}
