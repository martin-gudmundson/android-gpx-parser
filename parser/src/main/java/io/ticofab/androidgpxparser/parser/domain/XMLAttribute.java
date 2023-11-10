package io.ticofab.androidgpxparser.parser.domain;

/**
 * Created by Martin Gudmundson on 2023-11-06.
 * Copyright (c) 2023 Seedgate AB. All rights reserved.
 */

public class XMLAttribute {
    private String mName;
    private String mValue;
    private String mType;
    private String mPrefix;
    private String mNamespace;


    private XMLAttribute(XMLAttribute.Builder builder) {
        this.mName = builder.mName;
        this.mValue = builder.mValue;
        this.mType = builder.mType;
        this.mPrefix = builder.mPrefix;
        this.mNamespace = builder.mNamespace;
    }

    public String getName() {
        return this.mName;
    }

    public String getValue() {
        return this.mValue;
    }

    public String getType() {
        return this.mType;
    }

    public String getPrefix() {
        return this.mPrefix;
    }

    public String getNamespace() {
        return this.mNamespace;
    }


    public static class Builder {
        private String mName;
        private String mValue;
        private String mType;
        private String mPrefix;
        private String mNamespace;

        public XMLAttribute.Builder setName(String name) {
            mName = name;
            return this;
        }

        public XMLAttribute.Builder setValue(String value) {
            mValue = value;
            return this;
        }

        public XMLAttribute.Builder setType(String type) {
            mType = type;
            return this;
        }

        public XMLAttribute.Builder setPrefix(String prefix) {
            if (prefix == null) {
                mPrefix = "";
            } else {
                mPrefix = prefix;
            }
            return this;
        }

        public XMLAttribute.Builder setNamespace(String namespace) {
            mNamespace = namespace;
            return this;
        }

        public XMLAttribute build() {
            return new XMLAttribute(this);
        }
    }
}
