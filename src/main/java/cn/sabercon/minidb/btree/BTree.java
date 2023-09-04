package cn.sabercon.minidb.btree;

import cn.sabercon.minidb.base.FileBuffer;
import cn.sabercon.minidb.base.KeyValueDatabase;
import cn.sabercon.minidb.page.PageManager;
import cn.sabercon.minidb.page.PageType;
import cn.sabercon.minidb.util.Pair;
import cn.sabercon.minidb.util.Triple;
import com.google.common.base.Preconditions;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static cn.sabercon.minidb.btree.BTreeConstants.DEFAULT_ROOT_NODE;
import static cn.sabercon.minidb.btree.BTreeUtils.*;
import static cn.sabercon.minidb.page.PageConstants.*;

public class BTree implements KeyValueDatabase {

    private final PageManager pageManager;

    BTree(PageManager pageManager) {
        this.pageManager = pageManager;
    }

    public static BTree from(Path path) {
        var buffer = FileBuffer.from(path);
        return new BTree(PageManager.of(buffer));
    }

    private BTreeNode getRoot() {
        var root = pageManager.getRoot();
        return root == NULL_POINTER ? DEFAULT_ROOT_NODE : getNode(root);
    }

    private void setRoot(long root) {
        var oldRoot = pageManager.getRoot();
        if (oldRoot != NULL_POINTER && oldRoot != root) {
            deleteNode(oldRoot);
        }

        pageManager.setRoot(root);
        pageManager.flush();
    }

    private BTreeNode getNode(long pointer) {
        return BTreeNode.of(pageManager.getPage(pointer));
    }

    private void deleteNode(long pointer) {
        pageManager.deletePage(pointer);
    }

    private long createNode(BTreeNode node) {
        Preconditions.checkArgument(node.items() > 0);
        return pageManager.createPage(node.data());
    }

    private Pair<byte[], Long> save(BTreeNode node) {
        Preconditions.checkArgument(node.items() > 0);
        return Pair.of(node.getKey(0), createNode(node));
    }

    private List<Pair<byte[], Long>> save(List<BTreeNode> nodes) {
        return nodes.stream().map(this::save).toList();
    }

    private void updateRoot(BTreeNode node) {
        if (node.type() == PageType.BTREE_INTERNAL && node.items() == 1) {
            setRoot(node.getPointer(0));
        } else {
            var nodes = split(node);
            var newRoot = nodes.size() == 1 ? nodes.get(0) : createRoot(save(nodes));
            setRoot(createNode(newRoot));
        }
    }

    @Override
    public Optional<byte[]> find(byte[] key) {
        checkKeySize(key);

        return doFind(getRoot(), key);
    }

    private Optional<byte[]> doFind(BTreeNode node, byte[] key) {
        return switch (node.type()) {
            case BTREE_LEAF -> findInLeaf(node, key);
            case BTREE_INTERNAL -> findInInternal(node, key);
            default -> throw new AssertionError();
        };
    }

    private Optional<byte[]> findInInternal(BTreeNode node, byte[] key) {
        var index = node.lookUp(key);
        var pointer = node.getPointer(index);
        return doFind(getNode(pointer), key);
    }

    @Override
    public void upsert(byte[] key, byte[] value) {
        checkKeySize(key);
        checkValueSize(value);

        var updatedRoot = doUpsert(getRoot(), key, value);
        updateRoot(updatedRoot);
    }

    private BTreeNode doUpsert(BTreeNode node, byte[] key, byte[] value) {
        return switch (node.type()) {
            case BTREE_LEAF -> upsertInLeaf(node, key, value);
            case BTREE_INTERNAL -> upsertInInternal(node, key, value);
            default -> throw new AssertionError();
        };
    }

    private BTreeNode upsertInInternal(BTreeNode node, byte[] key, byte[] value) {
        var index = node.lookUp(key);
        var pointer = node.getPointer(index);
        var updatedKid = doUpsert(getNode(pointer), key, value);
        deleteNode(pointer);

        @SuppressWarnings("unchecked") Pair<byte[], Long>[] pointers = save(split(updatedKid)).toArray(Pair[]::new);
        return updateInInternal(node, index, pointers);
    }

    @Override
    public boolean delete(byte[] key) {
        checkKeySize(key);

        var deletionResult = doDelete(getRoot(), key);
        if (deletionResult.isEmpty()) return false;

        var updatedRoot = deletionResult.get();
        updateRoot(updatedRoot);
        return true;
    }

    private Optional<BTreeNode> doDelete(BTreeNode node, byte[] key) {
        return switch (node.type()) {
            case BTREE_LEAF -> deleteInLeaf(node, key);
            case BTREE_INTERNAL -> deleteInInternal(node, key);
            default -> throw new AssertionError();
        };
    }

    private Optional<BTreeNode> deleteInInternal(BTreeNode node, byte[] key) {
        var index = node.lookUp(key);
        var pointer = node.getPointer(index);
        var deletionResult = doDelete(getNode(pointer), key);
        if (deletionResult.isEmpty()) return Optional.empty();
        var updatedKid = deletionResult.get();
        deleteNode(pointer);

        var mergeableResult = mergeableSibling(node, updatedKid, index);
        if (mergeableResult.isPresent()) {
            var sibling = mergeableResult.get();
            var siblingIndex = sibling.first();
            var siblingPointer = sibling.second();
            var siblingNode = sibling.third();

            var merged = index > siblingIndex ? merge(siblingNode, updatedKid) : merge(updatedKid, siblingNode);
            deleteNode(siblingPointer);
            return Optional.of(updateInInternal(node, Math.min(index, siblingIndex), 2, save(merged)));
        } else if (updatedKid.items() == 0) {
            assert node.items() == 1;
            assert index == 0;
            return Optional.of(updateInInternal(node, index));
        } else {
            @SuppressWarnings("unchecked") Pair<byte[], Long>[] pointers = save(split(updatedKid)).toArray(Pair[]::new);
            return Optional.of(updateInInternal(node, index, pointers));
        }
    }

    private Optional<Triple<Integer, Long, BTreeNode>> mergeableSibling(BTreeNode parent, BTreeNode kid, int index) {
        if (kid.bytes() > PAGE_BYTE_SIZE / 4) {
            return Optional.empty();
        }

        for (var siblingIndex : List.of(index - 1, index + 1)) {
            if (siblingIndex < 0 || siblingIndex >= parent.items()) continue;

            var siblingPointer = parent.getPointer(siblingIndex);
            var sibling = getNode(siblingPointer);
            if (sibling.bytes() + kid.bytes() - HEADER_SIZE <= PAGE_BYTE_SIZE) {
                return Optional.of(Triple.of(siblingIndex, siblingPointer, sibling));
            }
        }

        return Optional.empty();
    }
}
