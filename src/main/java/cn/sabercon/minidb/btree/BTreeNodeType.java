package cn.sabercon.minidb.btree;

enum BTreeNodeType {

    INTERNAL(1),
    LEAF(2),
    FREE_LIST(3),
    ;

    private final int value;

    BTreeNodeType(int value) {
        this.value = value;
    }

    int value() {
        return value;
    }

    static BTreeNodeType of(int value) {
        return switch (value) {
            case 1 -> INTERNAL;
            case 2 -> LEAF;
            case 3 -> FREE_LIST;
            default -> throw new IllegalArgumentException("Unknown node type: " + value);
        };
    }
}
