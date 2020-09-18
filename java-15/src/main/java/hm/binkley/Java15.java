package hm.binkley;

import static java.lang.System.out;

public class Java15 {
    public static void main(String[] args) {
        Object x = 7;
        if (null != System.getProperty("java.version")) x = """
                uncle
                bob""";
        if (x instanceof String msg) out.println(new Fooby<>(3, msg));
    }
}

record Fooby<T>(int i, T s) {
    public Fooby {
        if (i < 0 || null == s) throw new IllegalArgumentException();
    }
}
