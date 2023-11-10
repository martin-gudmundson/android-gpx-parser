package io.ticofab.androidgpxparser.parser.domain;

import java.util.Collections;
import java.util.List;

/**
 * Created by Martin Gudmundson on 2023-11-06.
 * Copyright (c) 2023 Seedgate AB. All rights reserved.
 */

public class Extension {
    private String mName;
    private String mValue;

    private String mPrefix;
    private String mNamespace;

    private List<Extension> mChildren;

    private List<XMLAttribute> mAttributes;

    private Extension(Extension.Builder builder) {
        this.mName = builder.mName;
        this.mValue = builder.mValue;
        this.mPrefix = builder.mPrefix;
        this.mNamespace = builder.mNamespace;
        this.mChildren = Collections.unmodifiableList(builder.mChildren);
        this.mAttributes = Collections.unmodifiableList(builder.mAttributes);
    }

    public String getName() {
        return mName;
    }

    public String getValue() {
        return mValue;
    }

    public String getPrefix() {
        return mPrefix;
    }

    public String getNamespace() {
        return mNamespace;
    }

    public List<Extension> getChildren() {
        return mChildren;
    }

    public List<XMLAttribute> getAttributes() {
        return mAttributes;
    }

    public static class Builder {
        private String mName;
        private String mValue;

        private String mPrefix;
        private String mNamespace;

        private List<Extension> mChildren;
        private List<XMLAttribute> mAttributes;

        public Extension.Builder setName(String name) {
            mName = name;
            return this;
        }

        public Extension.Builder setValue(String value) {
            mValue = value;
            return this;
        }

        public Extension.Builder setPrefix(String prefix) {
            mPrefix = prefix;
            return this;
        }

        public Extension.Builder setNamespace(String namespace) {
            mNamespace = namespace;
            return this;
        }

        public Extension.Builder setChildren(List<Extension> children) {
            mChildren = children;
            return this;
        }

        public Extension.Builder setAttributes(List<XMLAttribute> attributes) {
            mAttributes = attributes;
            return this;
        }

        public Extension build() {
            return new Extension(this);
        }
    }
}
