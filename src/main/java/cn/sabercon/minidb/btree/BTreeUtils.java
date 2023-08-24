package cn.sabercon.minidb.btree;

import cn.sabercon.minidb.base.Conversions;
import cn.sabercon.minidb.base.Pair;
import com.google.common.base.Preconditions;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static cn.sabercon.minidb.base.PageNode.PAGE_SIZE;
import static cn.sabercon.minidb.btree.BTreeConstants.*;

final class BTreeUtils {

    private BTreeUtils() {
        throw new UnsupportedOperationException();
    }

    private static int bufferCapacity(int bytes) {
        Preconditions.checkArgument(bytes <= 2 * PAGE_SIZE);
        return bytes <= PAGE_SIZE ? PAGE_SIZE : 2 * PAGE_SIZE;
    }

    @SafeVarargs
    private static BTreeNode replace(BTreeNode node, int startIndex, int replaced, Pair<byte[], byte[]>... kvs) {
        var endIndex = startIndex + replaced;
        Objects.checkFromToIndex(startIndex, endIndex, node.keys());

        var addedBytes = (OFFSET_SIZE + LENGTH_SIZE) * kvs.length +
                Arrays.stream(kvs).mapToInt(kv -> kv.first().length + kv.second().length).sum();
        var replacedBytes = OFFSET_SIZE * replaced + node.getStartOffset(endIndex) - node.getStartOffset(startIndex);
        var newBytes = node.bytes() + addedBytes - replacedBytes;

        var newNode = BTreeNode.of(bufferCapacity(newBytes), node.type(), node.keys() + kvs.length - replaced);
        newNode.appendRange(0, node, 0, startIndex);
        newNode.appendValues(startIndex, List.of(kvs));
        newNode.appendRange(startIndex + kvs.length, node, endIndex, node.keys());
        return newNode;
    }

    static Optional<byte[]> findInLeaf(BTreeNode node, byte[] key) {
        Preconditions.checkArgument(node.type() == BTreeNodeType.LEAF);

        var index = node.lookUp(key);
        if (Arrays.equals(key, node.getKey(index))) {
            return Optional.of(node.getVal(index));
        } else {
            return Optional.empty();
        }
    }

    static BTreeNode upsertInLeaf(BTreeNode node, byte[] key, byte[] value) {
        Preconditions.checkArgument(node.type() == BTreeNodeType.LEAF);
        Preconditions.checkArgument(key.length <= MAX_KEY_SIZE);
        Preconditions.checkArgument(value.length <= MAX_VALUE_SIZE);

        var index = node.lookUp(key);
        var kv = Pair.of(key, value);
        if (Arrays.equals(key, node.getKey(index))) {
            return replace(node, index, 1, kv);
        } else {
            return replace(node, index + 1, 0, kv);
        }
    }

    static Optional<BTreeNode> deleteInLeaf(BTreeNode node, byte[] key) {
        Preconditions.checkArgument(node.type() == BTreeNodeType.LEAF);

        var index = node.lookUp(key);
        if (Arrays.equals(key, node.getKey(index))) {
            return Optional.of(replace(node, index, 1));
        } else {
            return Optional.empty();
        }
    }

    @SafeVarargs
    static BTreeNode updateInInternal(BTreeNode node, int index, int replaced, Pair<byte[], Long>... keyPointers) {
        Preconditions.checkArgument(node.type() == BTreeNodeType.INTERNAL);

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
        var root = BTreeNode.of(PAGE_SIZE, BTreeNodeType.INTERNAL, pointers.size());
        root.appendPointers(0, pointers);
        return root;
    }

    static List<BTreeNode> split(BTreeNode node) {
        Preconditions.checkArgument(node.bytes() + HEADER_SIZE <= 2 * PAGE_SIZE);

        if (node.bytes() <= PAGE_SIZE) {
            return List.of(node);
        }

        var nodes = split2(node);
        var left = nodes.first();
        var right = nodes.second();
        assert right.bytes() <= PAGE_SIZE;
        if (left.bytes() <= PAGE_SIZE) {
            return List.of(left, right);
        }

        var leftNodes = split2(left);
        assert leftNodes.first().bytes() <= PAGE_SIZE;
        assert leftNodes.second().bytes() <= PAGE_SIZE;
        return List.of(leftNodes.first(), leftNodes.second(), right);
    }

    private static Pair<BTreeNode, BTreeNode> split2(BTreeNode node) {
        Preconditions.checkArgument(node.keys() >= 2);
        var splitIndex = findSplitIndex(node);

        var leftBytes = HEADER_SIZE + OFFSET_SIZE * splitIndex + node.getStartOffset(splitIndex);
        var left = BTreeNode.of(bufferCapacity(leftBytes), node.type(), splitIndex);
        left.appendRange(0, node, 0, splitIndex);

        var right = BTreeNode.of(PAGE_SIZE, node.type(), node.keys() - splitIndex);
        right.appendRange(0, node, splitIndex, node.keys());

        return Pair.of(left, right);
    }

    private static int findSplitIndex(BTreeNode node) {
        // binary search
        var lo = 1;
        var hi = node.keys() - 1;
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
        Preconditions.checkArgument(left.bytes() + right.bytes() - HEADER_SIZE <= PAGE_SIZE);

        var newNode = BTreeNode.of(PAGE_SIZE, left.type(), left.keys() + right.keys());
        newNode.appendRange(0, left, 0, left.keys());
        newNode.appendRange(left.keys(), right, 0, right.keys());
        assert newNode.bytes() <= PAGE_SIZE;
        return newNode;
    }
}
