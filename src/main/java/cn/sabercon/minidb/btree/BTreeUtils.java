package cn.sabercon.minidb.btree;

import cn.sabercon.minidb.util.Conversions;
import cn.sabercon.minidb.util.Pair;
import com.google.common.base.Preconditions;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static cn.sabercon.minidb.btree.BTreeConstants.*;
import static cn.sabercon.minidb.page.PageConstants.HEADER_SIZE;
import static cn.sabercon.minidb.page.PageConstants.PAGE_BYTE_SIZE;
import static cn.sabercon.minidb.page.PageType.BTREE_INTERNAL;
import static cn.sabercon.minidb.page.PageType.BTREE_LEAF;

final class BTreeUtils {

    private BTreeUtils() {
        throw new UnsupportedOperationException();
    }

    private static int pageCount(int bytes) {
        Preconditions.checkArgument(bytes <= 2 * PAGE_BYTE_SIZE);
        return bytes <= PAGE_BYTE_SIZE ? 1 : 2;
    }

    static void checkKeySize(byte[] key) {
        Preconditions.checkArgument(key.length > 0 && key.length <= MAX_KEY_SIZE);
    }

    static void checkValueSize(byte[] value) {
        Preconditions.checkArgument(value.length <= MAX_VALUE_SIZE);
    }

    @SafeVarargs
    private static BTreeNode replace(BTreeNode node, int startIndex, int replaced, Pair<byte[], byte[]>... kvs) {
        var endIndex = startIndex + replaced;
        Objects.checkFromToIndex(startIndex, endIndex, node.items());

        var addedBytes = (OFFSET_SIZE + LENGTH_SIZE) * kvs.length +
                Arrays.stream(kvs).mapToInt(kv -> kv.first().length + kv.second().length).sum();
        var replacedBytes = OFFSET_SIZE * replaced + node.getStartOffset(endIndex) - node.getStartOffset(startIndex);
        var newBytes = node.bytes() + addedBytes - replacedBytes;

        var newNode = BTreeNode.of(node.type(), node.items() + kvs.length - replaced, pageCount(newBytes));
        newNode.appendRange(0, node, 0, startIndex);
        newNode.appendValues(startIndex, List.of(kvs));
        newNode.appendRange(startIndex + kvs.length, node, endIndex, node.items());
        return newNode;
    }

    static Optional<byte[]> findInLeaf(BTreeNode node, byte[] key) {
        Preconditions.checkArgument(node.type() == BTREE_LEAF);
        checkKeySize(key);

        var index = node.lookUp(key);
        if (Arrays.equals(key, node.getKey(index))) {
            return Optional.of(node.getVal(index));
        } else {
            return Optional.empty();
        }
    }

    static BTreeNode upsertInLeaf(BTreeNode node, byte[] key, byte[] value) {
        Preconditions.checkArgument(node.type() == BTREE_LEAF);
        checkKeySize(key);
        checkValueSize(value);

        var index = node.lookUp(key);
        var kv = Pair.of(key, value);
        if (Arrays.equals(key, node.getKey(index))) {
            return replace(node, index, 1, kv);
        } else {
            return replace(node, index + 1, 0, kv);
        }
    }

    static Optional<BTreeNode> deleteInLeaf(BTreeNode node, byte[] key) {
        Preconditions.checkArgument(node.type() == BTREE_LEAF);
        checkKeySize(key);

        var index = node.lookUp(key);
        if (Arrays.equals(key, node.getKey(index))) {
            return Optional.of(replace(node, index, 1));
        } else {
            return Optional.empty();
        }
    }

    @SafeVarargs
    static BTreeNode updateInInternal(BTreeNode node, int index, int replaced, Pair<byte[], Long>... keyPointers) {
        Preconditions.checkArgument(node.type() == BTREE_INTERNAL);

        @SuppressWarnings("unchecked") Pair<byte[], byte[]>[] kvs = Arrays.stream(keyPointers)
                .map(kv -> Pair.of(kv.first(), Conversions.toBytes(kv.second())))
                .toArray(Pair[]::new);
        return replace(node, index, replaced, kvs);
    }

    @SafeVarargs
    static BTreeNode updateInInternal(BTreeNode node, int index, Pair<byte[], Long>... keyPointers) {
        return updateInInternal(node, index, 1, keyPointers);
    }

    static BTreeNode createRoot(List<Pair<byte[], Long>> pointers) {
        var root = BTreeNode.of(BTREE_INTERNAL, pointers.size(), 1);
        root.appendPointers(0, pointers);
        return root;
    }

    static List<BTreeNode> split(BTreeNode node) {
        Preconditions.checkArgument(node.bytes() + HEADER_SIZE <= 2 * PAGE_BYTE_SIZE);

        if (node.bytes() <= PAGE_BYTE_SIZE) {
            return List.of(node);
        }

        var nodes = split2(node);
        var left = nodes.first();
        var right = nodes.second();
        assert right.bytes() <= PAGE_BYTE_SIZE;
        if (left.bytes() <= PAGE_BYTE_SIZE) {
            return List.of(left, right);
        }

        var leftNodes = split2(left);
        assert leftNodes.first().bytes() <= PAGE_BYTE_SIZE;
        assert leftNodes.second().bytes() <= PAGE_BYTE_SIZE;
        return List.of(leftNodes.first(), leftNodes.second(), right);
    }

    private static Pair<BTreeNode, BTreeNode> split2(BTreeNode node) {
        Preconditions.checkArgument(node.items() >= 2);

        var splitIndex = findSplitIndex(node);

        var leftBytes = HEADER_SIZE + OFFSET_SIZE * splitIndex + node.getStartOffset(splitIndex);
        var left = BTreeNode.of(node.type(), splitIndex, pageCount(leftBytes));
        left.appendRange(0, node, 0, splitIndex);

        var right = BTreeNode.of(node.type(), node.items() - splitIndex, 1);
        right.appendRange(0, node, splitIndex, node.items());

        return Pair.of(left, right);
    }

    private static int findSplitIndex(BTreeNode node) {
        // Uses binary search
        var lo = 1;
        var hi = node.items() - 1;
        var target = (node.bytes() - HEADER_SIZE + 1) / 2;
        while (lo < hi) {
            var mid = (lo + hi) / 2;
            var bytes = OFFSET_SIZE * mid + node.getStartOffset(mid);
            if (bytes < target) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return lo;
    }

    static BTreeNode merge(BTreeNode left, BTreeNode right) {
        Preconditions.checkArgument(left.type() == right.type());
        Preconditions.checkArgument(left.bytes() + right.bytes() - HEADER_SIZE <= PAGE_BYTE_SIZE);

        var newNode = BTreeNode.of(left.type(), left.items() + right.items(), 1);
        newNode.appendRange(0, left, 0, left.items());
        newNode.appendRange(left.items(), right, 0, right.items());
        return newNode;
    }
}
