package com.whiker.learn.common.dynamiccompile;

/**
 * @author whiker@163.com create on 16-5-7.
 */
public class DynamicCompiler {

    private static final DynamicCompilerImpl IMPL = new DynamicCompilerImpl();

    public static Class<?> compile(String fullClassName, String sourceCode) throws Throwable {
        return IMPL.compile(fullClassName, sourceCode);
    }
}
