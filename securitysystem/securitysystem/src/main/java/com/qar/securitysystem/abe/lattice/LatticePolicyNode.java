package com.qar.securitysystem.abe.lattice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LatticePolicyNode {
    public enum Type {
        AND,
        OR,
        LEAF
    }

    private int nodeId;
    private Type type;
    private String attribute;
    private List<LatticePolicyNode> children = new ArrayList<>();

    public int getNodeId() {
        return nodeId;
    }

    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public List<LatticePolicyNode> getChildren() {
        return children;
    }

    public void setChildren(List<LatticePolicyNode> children) {
        this.children = children == null ? new ArrayList<>() : children;
    }

    public boolean isLeaf() {
        return type == Type.LEAF;
    }

    @JsonProperty("leaf")
    public void setLeaf(boolean ignored) {
    }

    @JsonIgnore
    public boolean getLeaf() {
        return isLeaf();
    }

    public static LatticePolicyNode leaf(int nodeId, String attribute) {
        LatticePolicyNode node = new LatticePolicyNode();
        node.setNodeId(nodeId);
        node.setType(Type.LEAF);
        node.setAttribute(attribute);
        return node;
    }

    public static LatticePolicyNode branch(int nodeId, Type type, List<LatticePolicyNode> children) {
        LatticePolicyNode node = new LatticePolicyNode();
        node.setNodeId(nodeId);
        node.setType(type);
        node.setChildren(children);
        return node;
    }
}
