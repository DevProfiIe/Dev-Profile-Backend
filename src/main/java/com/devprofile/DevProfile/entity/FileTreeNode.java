package com.devprofile.DevProfile.entity;

import java.util.ArrayList;
import java.util.List;

public class FileTreeNode {
    private String id;

    public String name;
    String type;
    public List<FileTreeNode> children;

    public FileTreeNode(String name, String type) {
        this.name = name;
        this.type = type;
        if (this.name.isEmpty()) {
            this.children = new ArrayList<>();
        } else {
            this.children = null;
        }
    }

    public List<FileTreeNode> getChildren() {
        return children != null ? children : new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}