package com.whiker.learn.common.test.dynamiccompile;

import com.whiker.learn.common.dynamiccompile.ClassUtil;
import com.whiker.learn.common.dynamiccompile.DynamicCompiler;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author whiker@163.com create on 16-5-7.
 */
public class DynamicCompilerTest {
    private final String packageName = this.getClass().getPackage().getName();

    @Test
    public void test() throws Throwable {
        Class<?> fooClass = DynamicCompiler.compile(packageName + ".FooImpl", FOO_CLASS_CODE);

        Foo fooEn = (Foo) ClassUtil.newInstance(fooClass, "hello");
        assertEquals("hello, whiker", fooEn.say("whiker"));
        Foo fooCh = (Foo) ClassUtil.newInstance(fooClass, "你好");
        assertEquals("你好, whiker", fooCh.say("whiker"));
    }

    private final String FOO_CLASS_CODE = "package " + packageName + ";\n" // 必须要加包
            + "public class FooImpl implements " + Foo.class.getCanonicalName() + " {\n"
            + "\tprivate String term;\n" + "\tpublic FooImpl(String term) {\n\t\tthis.term = term;\n\t}\n"
            + "\tpublic String say(String name) {\n\t\treturn this.term + \", \" + name;\n\t}\n" + "}";

    public interface Foo {
        String say(String name);
    }
}
