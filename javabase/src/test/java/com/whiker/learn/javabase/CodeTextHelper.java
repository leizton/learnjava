package com.whiker.learn.javabase;

/**
 * @author leizton create on 17-12-15.
 */
public class CodeTextHelper {

    private interface Source {
        boolean hasNext();

        String next();
    }

    private interface Sink {
        void receive(String s);

        void complete();
    }

    private static final class FileLineSource implements Source {

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public String next() {
            return null;
        }
    }

    private static final class FileSink implements Sink {

        @Override
        public void receive(String s) {
        }

        @Override
        public void complete() {
        }
    }
}
