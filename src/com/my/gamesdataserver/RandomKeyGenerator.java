package com.my.gamesdataserver;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;

public class RandomKeyGenerator {
    private static final String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static final String lower = upper.toLowerCase(Locale.ROOT);

    private static final String digits = "0123456789";

    private static final String symbols = upper + lower + digits;

    private Random random;

    private final char[] chars = symbols.toCharArray();

    private char[] buf;

    public RandomKeyGenerator() {
        this.random = Objects.requireNonNull(new SecureRandom());
    }
    
    public String nextString(int length) {
		this.buf = new char[length];
        for (int idx = 0; idx < buf.length; ++idx) {
            buf[idx] = chars[random.nextInt(chars.length)];
        }
        return new String(buf);
    }
}
