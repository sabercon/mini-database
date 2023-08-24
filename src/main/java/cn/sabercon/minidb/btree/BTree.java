package cn.sabercon.minidb.btree;

import cn.sabercon.minidb.base.PageStore;
import cn.sabercon.minidb.base.Pair;
import cn.sabercon.minidb.base.Triple;
import com.google.common.base.Preconditions;

import java.util.List;
import java.util.Optional;

import static cn.sabercon.minidb.base.PageNode.PAGE_SIZE;
import static cn.sabercon.minidb.btree.BTreeConstants.DEFAULT_ROOT_NODE;
import static cn.sabercon.minidb.btree.BTreeConstants.HEADER_SIZE;
import static cn.sabercon.minidb.btree.BTreeUtils.*;

public class BTree {

    private final PageStore store;

    public BTree(PageStore store) {
        this.store = store;
    }

    private BTreeNode getRoot() {
        var root = store.getRoot();
        return root == 0 ? DEFAULT_ROOT_NODE : getNode(root);
    }

    private void setRoot(long pointer) {
        store.setRoot(pointer);
    }

    private void setRoot(BTreeNode node) {
        setRoot(createNode(node));
    }

    private BTreeNode getNode(long pointer) {
        return BTreeNode.of(store.getData(pointer));
    }

    private long createNode(BTreeNode node) {
        return store.createPage(node.data());
    }

    private void deleteNode(long pointer) {
        store.deletePage(pointer);
    }

    private Pair<byte[], Long> save(BTreeNode node) {
        Preconditions.checkArgument(node.keys() > 0);
        return Pair.of(node.getKey(0), createNode(node));
    }

    private List<Pair<byte[], Long>> save(List<BTreeNode> nodes) {
        return nodes.stream().map(this::save).toList();
    }

    public Optional<byte[]> find(byte[] key) {
        Preconditions.checkArgument(key.length > 0);

        var root = getRoot();
        return doFind(root, key);
    }

    private Optional<byte[]> doFind(BTreeNode node, byte[] key) {
        return switch (node.type()) {
            case LEAF -> findInLeaf(node, key);
            case INTERNAL -> findInInternal(node, key);
            default -> throw new AssertionError();
        };
    }

    private Optional<byte[]> findInInternal(BTreeNode node, byte[] key) {
        var index = node.lookUp(key);
        var pointer = node.getPointer(index);
        return doFind(getNode(pointer), key);
    }

    public void upsert(byte[] key, byte[] value) {
        Preconditions.checkArgument(key.length > 0);

        var root = getRoot();
        var updatedRoot = doUpsert(root, key, value);

        var nodes = split(updatedRoot);
        var newRoot = nodes.size() == 1 ? nodes.get(0) : createRoot(save(nodes));
        setRoot(newRoot);
    }

    private BTreeNode doUpsert(BTreeNode node, byte[] key, byte[] value) {
        return switch (node.type()) {
            case LEAF -> upsertInLeaf(node, key, value);
            case INTERNAL -> upsertInInternal(node, key, value);
            default -> throw new AssertionError();
        };
    }

    private BTreeNode upsertInInternal(BTreeNode node, byte[] key, byte[] value) {
        var index = node.lookUp(key);
        var pointer = node.getPointer(index);
        var updatedKid = doUpsert(getNode(pointer), key, value);
        @SuppressWarnings("unchecked") Pair<byte[], Long>[] pointers = save(split(updatedKid)).toArray(Pair[]::new);
        return updateInInternal(node, index, pointers);
    }

    public boolean delete(byte[] key) {
        Preconditions.checkArgument(key.length > 0);

        var root = getRoot();
        var deletionResult = doDelete(root, key);
        if (deletionResult.isEmpty()) return false;
        var updatedRoot = deletionResult.get();

        if (updatedRoot.type() == BTreeNodeType.INTERNAL && updatedRoot.keys() == 1) {
            setRoot(updatedRoot.getPointer(0));
        } else {
            setRoot(updatedRoot);
        }
        return true;
    }

    private Optional<BTreeNode> doDelete(BTreeNode node, byte[] key) {
        return switch (node.type()) {
            case LEAF -> deleteInLeaf(node, key);
            case INTERNAL -> deleteInInternal(node, key);
            default -> throw new AssertionError();
        };
    }

    private Optional<BTreeNode> deleteInInternal(BTreeNode node, byte[] key) {
        var index = node.lookUp(key);
        var pointer = node.getPointer(index);
        var deletionResult = doDelete(getNode(pointer), key);
        if (deletionResult.isEmpty()) return Optional.empty();
        var updatedKid = deletionResult.get();

        var mergeableResult = mergeableSibling(node, updatedKid, index);
        if (mergeableResult.isPresent()) {
            var sibling = mergeableResult.get();
            var siblingIndex = sibling.first();
            var siblingPointer = sibling.second();
            var siblingNode = sibling.third();
            var merged = index > siblingIndex ? merge(siblingNode, updatedKid) : merge(updatedKid, siblingNode);
            return Optional.of(updateInInternal(node, Math.min(index, siblingIndex), 2, save(merged)));
        } else if (updatedKid.keys() == 0) {
            assert node.keys() == 1;
            assert index == 0;
            return Optional.of(updateInInternal(node, index));
        } else {
            return Optional.of(updateInInternal(node, index, save(updatedKid)));
        }
    }

    private Optional<Triple<Integer, Long, BTreeNode>> mergeableSibling(BTreeNode parent, BTreeNode kid, int index) {
        if (kid.bytes() > PAGE_SIZE / 4) {
            return Optional.empty();
        }

        if (index > 0) {
            var leftPointer = parent.getPointer(index - 1);
            var left = getNode(leftPointer);
            if (left.bytes() + kid.bytes() - HEADER_SIZE <= PAGE_SIZE) {
                return Optional.of(Triple.of(index - 1, leftPointer, left));
            }
        }

        if (index < parent.keys() - 1) {
            var rightPointer = parent.getPointer(index + 1);
            var right = getNode(rightPointer);
            if (right.bytes() + kid.bytes() - HEADER_SIZE <= PAGE_SIZE) {
                return Optional.of(Triple.of(index + 1, rightPointer, right));
            }
        }

        return Optional.empty();
    }
}
