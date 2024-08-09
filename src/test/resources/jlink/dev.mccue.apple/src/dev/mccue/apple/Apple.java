package dev.mccue.apple;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;

public final class Apple {
    public String color() {
        try {
            return new String(
                    Objects.requireNonNull(
                            Apple.class.getResourceAsStream("/dev/mccue/apple/color.txt")
                    ).readAllBytes()
            );
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

    }
}
