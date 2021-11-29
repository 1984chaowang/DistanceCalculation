package test;

import java.time.Instant;

public class test {
    public static class Test {
        public static void main(String[] args) {
            System.out.println(getTime());

            for (int i=0; i<1000000; i++) {
                getTime();
            }

            System.out.println(getTime());
        }

        private static long getTime() {
            return Instant.now().toEpochMilli();
        }
    }
}
