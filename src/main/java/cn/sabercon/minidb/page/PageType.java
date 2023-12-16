package cn.sabercon.minidb.page;

public enum PageType {

    BTREE_INTERNAL(1),
    BTREE_LEAF(2),
    FREE_LIST(3),
    ;

    private final int value;

    PageType(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static PageType of(int value) {
        return switch (value) {
            case 1 -> BTREE_INTERNAL;
            case 2 -> BTREE_LEAF;
            case 3 -> FREE_LIST;
            default -> throw new IllegalArgumentException(STR."Unknown node type: \{value}");
        };
    }
}
