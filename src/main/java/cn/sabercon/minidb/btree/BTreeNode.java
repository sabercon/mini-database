package cn.sabercon.minidb.btree;

import cn.sabercon.minidb.base.Conversions;
import cn.sabercon.minidb.base.PageNode;
import cn.sabercon.minidb.base.Pair;
import com.google.common.base.Preconditions;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static cn.sabercon.minidb.btree.BTreeConstants.*;

class BTreeNode extends PageNode {

    private BTreeNode(ByteBuffer data) {
        super(data);
    }

    static BTreeNode of(ByteBuffer data) {
        return new BTreeNode(data);
    }

    static BTreeNode of(int capacity, BTreeNodeType type, int keys) {
        var node = new BTreeNode(ByteBuffer.allocate(capacity));
        node.putInt(0, type.value());
        node.putInt(NODE_TYPE_SIZE, keys);
        return node;
    }

    BTreeNodeType type() {
        return BTreeNodeType.of(getInt(0));
    }

    int keys() {
        return getInt(NODE_TYPE_SIZE);
    }

    int offsetPos(int index) {
        Objects.checkIndex(index, keys());
        return HEADER_SIZE + OFFSET_SIZE * index;
    }

    int getStartOffset(int index) {
        return index == 0 ? 0 : getEndOffset(index - 1);
    }

    int getEndOffset(int index) {
        return getInt(offsetPos(index));
    }

    private int kvStartPos(int index) {
        return HEADER_SIZE + OFFSET_SIZE * keys() + getStartOffset(index);
    }

    private int kvEndPos(int index) {
        return HEADER_SIZE + OFFSET_SIZE * keys() + getEndOffset(index);
    }

    byte[] getKey(int index) {
        var kvStartPos = kvStartPos(index);
        var keyStartPos = kvStartPos + LENGTH_SIZE;
        var keyLength = getInt(kvStartPos);
        return getBytes(keyStartPos, keyLength);
    }

    byte[] getVal(int index) {
        var kvStartPos = kvStartPos(index);
        var kvEndPos = kvEndPos(index);
        var keyLength = getInt(kvStartPos);
        var valStartPos = kvStartPos + LENGTH_SIZE + keyLength;
        var valLength = kvEndPos - valStartPos;
        return getBytes(valStartPos, valLength);
    }

    long getPointer(int index) {
        return Conversions.toLong(getVal(index));
    }

    void appendValue(int index, byte[] key, byte[] val) {
        Objects.checkIndex(index, keys());

        // Set offset
        var endOffset = getStartOffset(index) + LENGTH_SIZE + key.length + val.length;
        putInt(offsetPos(index), endOffset);

        // Set key-value pair
        var kvStartPos = kvStartPos(index);
        putInt(kvStartPos, key.length);
        putBytes(kvStartPos + LENGTH_SIZE, key);
        putBytes(kvStartPos + LENGTH_SIZE + key.length, val);
    }

    void appendPointer(int index, byte[] key, long pointer) {
        appendValue(index, key, Conversions.toBytes(pointer));
    }

    void appendValues(int index, List<Pair<byte[], byte[]>> keyValues) {
        for (Pair(byte[] k, byte[] v) : keyValues) {
            appendValue(index, k, v);
            index += 1;
        }
    }

    void appendPointers(int index, List<Pair<byte[], Long>> keyPointers) {
        for (Pair(byte[] k, Long p) : keyPointers) {
            appendPointer(index, k, p);
            index += 1;
        }
    }

    void appendRange(int index, BTreeNode src, int start, int end) {
        Objects.checkFromToIndex(start, end, src.keys());
        Preconditions.checkArgument(index + end - start <= keys());

        // Copies offsets
        var offsetDiff = getStartOffset(index) - src.getStartOffset(start);
        for (int dstI = index, srcI = start; srcI < end; dstI++, srcI++) {
            var srcEndOffset = src.getEndOffset(srcI);
            var dstEndOffset = srcEndOffset + offsetDiff;
            putInt(offsetPos(dstI), dstEndOffset);
        }

        // Copies key-value pairs
        var srcKvStartPos = src.kvStartPos(start);
        var srcKvEndPos = src.kvStartPos(end);
        var kvsLength = srcKvEndPos - srcKvStartPos;
        copy(kvStartPos(index), src, srcKvStartPos, kvsLength);
    }

    /**
     * @return Node size in bytes
     */
    int bytes() {
        return kvStartPos(keys());
    }

    /**
     * Note that for the result to be correct the first key must not be greater than the given key.
     *
     * @return The index of the greatest key that is less than or equal to the given key
     */
    int lookUp(byte[] key) {
        // binary search
        var lo = 0;
        var hi = keys();
        while (lo < hi - 1) {
            var mid = (lo + hi) / 2;
            var comparison = Arrays.compare(getKey(mid), key);
            if (comparison < 0) {
                lo = mid;
            } else if (comparison > 0) {
                hi = mid;
            } else {
                return mid;
            }
        }
        return lo;
    }
}
