package cn.sabercon.minidb.btree;

import cn.sabercon.minidb.util.Conversions;
import com.google.common.base.Preconditions;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import static cn.sabercon.minidb.btree.BTreeConstants.POINTER_SIZE;

record BTreeMaster(long root, long flushed) {

    static final int SIGNATURE_SIZE = 16;

    static final int TOTAL_SIZE = SIGNATURE_SIZE + 2 * POINTER_SIZE;

    static final MemorySegment SIGNATURE = MemorySegment.ofArray(Conversions.toBytes("MINIDB-SIGNATURE"));

    BTreeMaster {
        Preconditions.checkArgument(root >= 0);
        Preconditions.checkArgument(flushed >= 1);
    }

    static BTreeMaster from(MemorySegment data) {
        Preconditions.checkArgument(SIGNATURE.mismatch(data.asSlice(0, SIGNATURE_SIZE)) < 0);
        var root = data.get(ValueLayout.JAVA_LONG, SIGNATURE_SIZE);
        var flushed = data.get(ValueLayout.JAVA_LONG, SIGNATURE_SIZE + POINTER_SIZE);
        return new BTreeMaster(root, flushed);
    }

    MemorySegment data() {
        var data = MemorySegment.ofArray(new byte[TOTAL_SIZE]);
        data.copyFrom(SIGNATURE);
        data.set(ValueLayout.JAVA_LONG, SIGNATURE_SIZE, root);
        data.set(ValueLayout.JAVA_LONG, SIGNATURE_SIZE + POINTER_SIZE, flushed);
        return data;
    }
}
