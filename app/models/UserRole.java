package models;

public enum UserRole {
    ADMIN, NORMAL, ANONYMOUS;

    private static final UserRole byIndex[] = UserRole.class.getEnumConstants();

    public static UserRole byIndex(int index) {
        return byIndex[index];
    }
}
