/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

import java.math.BigInteger;
import java.util.*;

import static java.util.Objects.requireNonNull;

/**
 * Generic version implementation based on the Maven implementation.
 *
 * @since 2.0
 */
// https://github.com/apache/maven-resolver/blob/98126539f3c66fc4ab50b178c2eb4b8fd169fd72/maven-resolver-util/src/main/java/org/eclipse/aether/util/version/GenericVersion.java
public class VersionGeneric implements Version {
    private final String version_;
    private final List<Item> items_;
    private final int hash_;

    @Override
    public String qualifier() {
        return "";
    }

    @Override
    public Version withQualifier(String qualifier) {
        return new VersionGeneric(version_);
    }

    @Override
    public boolean isSnapshot() {
        return false;
    }

    /**
     * Creates a generic version from the specified string.
     *
     * @param version The version string, must not be {@code null}.
     * @since 2.0
     */
    public VersionGeneric(String version) {
        version_ = requireNonNull(version, "version cannot be null");
        items_ = parse(version);
        hash_ = items_.hashCode();
    }

    /**
     * Visible for testing.
     */
    List<Item> asItems() {
        return items_;
    }

    private static List<Item> parse(String version) {
        var items = new ArrayList<Item>();

        for (var tokenizer = new Tokenizer(version); tokenizer.next(); ) {
            var item = tokenizer.toItem();
            items.add(item);
        }

        trimPadding(items);

        return Collections.unmodifiableList(items);
    }

    /**
     * Visible for testing.
     */
    static void trimPadding(List<Item> items) {
        Boolean number = null;
        var end = items.size() - 1;
        for (var i = end; i > 0; i--) {
            var item = items.get(i);
            if (!Boolean.valueOf(item.isNumber()).equals(number)) {
                end = i;
                number = item.isNumber();
            }
            if (end == i
                && (i == items.size() - 1 || items.get(i - 1).isNumber() == item.isNumber())
                && item.compareTo(null) == 0) {
                items.remove(i);
                end--;
            }
        }
    }

    @Override
    public int compareTo(Version other) {
        VersionGeneric generic;
        if (other instanceof VersionGeneric) {
            generic = (VersionGeneric)other;
        }
        else {
            generic = new VersionGeneric(other.toString());
        }

        final var these = items_;
        final var those = generic.items_;

        var number = true;

        for (var index = 0; ; index++) {
            if (index >= these.size() && index >= those.size()) {
                return 0;
            } else if (index >= these.size()) {
                return -comparePadding(those, index, null);
            } else if (index >= those.size()) {
                return comparePadding(these, index, null);
            }

            var thisItem = these.get(index);
            var thatItem = those.get(index);

            if (thisItem.isNumber() != thatItem.isNumber()) {
                if (index == 0) {
                    return thisItem.compareTo(thatItem);
                }
                if (number == thisItem.isNumber()) {
                    return comparePadding(these, index, number);
                } else {
                    return -comparePadding(those, index, number);
                }
            } else {
                var rel = thisItem.compareTo(thatItem);
                if (rel != 0) {
                    return rel;
                }
                number = thisItem.isNumber();
            }
        }
    }

    private static int comparePadding(List<Item> items, int index, Boolean number) {
        var rel = 0;
        for (var i = index; i < items.size(); i++) {
            var item = items.get(i);
            if (number != null && number != item.isNumber()) {
                // do not stop here, but continue, skipping non-number members
                continue;
            }
            rel = item.compareTo(null);
            if (rel != 0) {
                break;
            }
        }
        return rel;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof VersionGeneric) && compareTo((VersionGeneric) obj) == 0;
    }

    @Override
    public int hashCode() {
        return hash_;
    }

    @Override
    public String toString() {
        return version_;
    }

    static final class Tokenizer {
        private static final Integer QUALIFIER_ALPHA = -5;
        private static final Integer QUALIFIER_BETA = -4;
        private static final Integer QUALIFIER_MILESTONE = -3;
        private static final Map<String, Integer> QUALIFIERS;

        static {
            QUALIFIERS = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            QUALIFIERS.put("alpha", QUALIFIER_ALPHA);
            QUALIFIERS.put("beta", QUALIFIER_BETA);
            QUALIFIERS.put("milestone", QUALIFIER_MILESTONE);
            QUALIFIERS.put("cr", -2);
            QUALIFIERS.put("rc", -2);
            QUALIFIERS.put("snapshot", -1);
            QUALIFIERS.put("ga", 0);
            QUALIFIERS.put("final", 0);
            QUALIFIERS.put("release", 0);
            QUALIFIERS.put("", 0);
            QUALIFIERS.put("sp", 1);
        }

        private final String version_;
        private final int versionLength_;
        private int index_;
        private String token_;
        private boolean number_;
        private boolean terminatedByNumber_;

        Tokenizer(String version) {
            version_ = (!version.isEmpty()) ? version : "0";
            versionLength_ = this.version_.length();
        }

        public boolean next() {
            if (index_ >= versionLength_) {
                return false;
            }

            var state = -2;

            var start = index_;
            var end = versionLength_;
            terminatedByNumber_ = false;

            for (; index_ < versionLength_; index_++) {
                var c = version_.charAt(index_);

                if (c == '.' || c == '-' || c == '_') {
                    end = index_;
                    index_++;
                    break;
                } else {
                    var digit = Character.digit(c, 10);
                    if (digit >= 0) {
                        if (state == -1) {
                            end = index_;
                            terminatedByNumber_ = true;
                            break;
                        }
                        if (state == 0) {
                            // normalize numbers and strip leading zeros (prereq for Integer/BigInteger handling)
                            start++;
                        }
                        state = (state > 0 || digit > 0) ? 1 : 0;
                    } else {
                        if (state >= 0) {
                            end = index_;
                            break;
                        }
                        state = -1;
                    }
                }
            }

            if (end - start > 0) {
                token_ = version_.substring(start, end);
                number_ = state >= 0;
            } else {
                token_ = "0";
                number_ = true;
            }

            return true;
        }

        @Override
        public String toString() {
            return String.valueOf(token_);
        }

        public Item toItem() {
            if (number_) {
                try {
                    if (token_.length() < 10) {
                        return new Item(Item.KIND_INT, Integer.parseInt(token_));
                    } else {
                        return new Item(Item.KIND_BIGINT, new BigInteger(token_));
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalStateException(e);
                }
            } else {
                if (index_ >= version_.length()) {
                    if ("min".equalsIgnoreCase(token_)) {
                        return Item.MIN;
                    } else if ("max".equalsIgnoreCase(token_)) {
                        return Item.MAX;
                    }
                }
                if (terminatedByNumber_ && token_.length() == 1) {
                    switch (token_.charAt(0)) {
                        case 'a':
                        case 'A':
                            return new Item(Item.KIND_QUALIFIER, QUALIFIER_ALPHA);
                        case 'b':
                        case 'B':
                            return new Item(Item.KIND_QUALIFIER, QUALIFIER_BETA);
                        case 'm':
                        case 'M':
                            return new Item(Item.KIND_QUALIFIER, QUALIFIER_MILESTONE);
                        default:
                    }
                }
                var qualifier = QUALIFIERS.get(token_);
                if (qualifier != null) {
                    return new Item(Item.KIND_QUALIFIER, qualifier);
                } else {
                    return new Item(Item.KIND_STRING, token_.toLowerCase(Locale.ENGLISH));
                }
            }
        }
    }

    static final class Item {
        static final int KIND_MAX = 8;
        static final int KIND_BIGINT = 5;
        static final int KIND_INT = 4;
        static final int KIND_STRING = 3;
        static final int KIND_QUALIFIER = 2;
        static final int KIND_MIN = 0;
        static final Item MAX = new Item(KIND_MAX, "max");
        static final Item MIN = new Item(KIND_MIN, "min");

        private final int kind_;
        private final Object value_;

        Item(int kind, Object value) {
            kind_ = kind;
            value_ = value;
        }

        public boolean isNumber() {
            return (kind_ & KIND_QUALIFIER) == 0; // i.e. kind != string/qualifier
        }

        public int compareTo(Item that) {
            int rel;
            if (that == null) {
                // null in this context denotes the pad item (0 or "ga")
                rel = switch (kind_) {
                    case KIND_MIN -> -1;
                    case KIND_MAX, KIND_BIGINT, KIND_STRING -> 1;
                    case KIND_INT, KIND_QUALIFIER -> (Integer) value_;
                    default -> throw new IllegalStateException("unknown version item kind " + kind_);
                };
            } else {
                rel = kind_ - that.kind_;
                if (rel == 0) {
                    switch (kind_) {
                        case KIND_MAX:
                        case KIND_MIN:
                            break;
                        case KIND_BIGINT:
                            rel = ((BigInteger) value_).compareTo((BigInteger) that.value_);
                            break;
                        case KIND_INT:
                        case KIND_QUALIFIER:
                            rel = ((Integer) value_).compareTo((Integer) that.value_);
                            break;
                        case KIND_STRING:
                            rel = ((String) value_).compareToIgnoreCase((String) that.value_);
                            break;
                        default:
                            throw new IllegalStateException("unknown version item kind " + kind_);
                    }
                }
            }
            return rel;
        }

        @Override
        public boolean equals(Object obj) {
            return (obj instanceof Item) && compareTo((Item) obj) == 0;
        }

        @Override
        public int hashCode() {
            return value_.hashCode() + kind_ * 31;
        }

        @Override
        public String toString() {
            return String.valueOf(value_);
        }
    }
}
