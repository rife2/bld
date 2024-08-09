package dev.mccue.tree;

import dev.mccue.apple.Apple;

public final class Tree {
    public Apple bearFruit() {
        return new Apple();
    }

    public static void main(String[] args) {
        System.out.println(
                new Tree().bearFruit().color()
        );
    }
}
