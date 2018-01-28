package com.whiker.learn.javabase;

/**
 * Created by whiker on 2018/1/28.
 */
public class ExceptionTest {

    private static final class HandleException extends Exception {
        static final HandleException INSTANCE = new HandleException("static-ex");

        HandleException(String msg) {
            super(msg);
        }
    }

    private static void handleA() throws HandleException {
        throw new HandleException("handleA");
    }

    private static void handle() throws HandleException {
        throw HandleException.INSTANCE;
    }

    public static void main(String[] args) {
        if (HandleException.INSTANCE == null) {
            return;
        }

        try {
            handle();
        } catch (HandleException e) {
            e.printStackTrace();
        }

        try {
            handleA();
        } catch (HandleException e) {
            e.printStackTrace();
        }

        try {
            handle();
        } catch (HandleException e) {
            /*
             * 异常栈和第一次相同
             * 在Throwable的构造方法里fillInStackTrace()就初始化了异常栈
             */
            e.printStackTrace();
        }
    }
}
