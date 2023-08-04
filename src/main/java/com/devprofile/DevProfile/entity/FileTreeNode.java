package com.devprofile.DevProfile.entity;

import java.util.ArrayList;
import java.util.List;

public class FileTreeNode {
    public String name;
    String type;
    public List<FileTreeNode> children;

    public FileTreeNode(String name, String type) {
        this.name = name;
        this.type = type;
        this.children = new ArrayList<>();
    }
}