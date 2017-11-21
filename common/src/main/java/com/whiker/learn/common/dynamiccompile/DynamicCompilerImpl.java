package com.whiker.learn.common.dynamiccompile;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import javax.tools.*;
import java.io.*;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * @author whiker@163.com create on 16-5-7.
 */
final class DynamicCompilerImpl {

    private final ClassLoaderImpl classLoader;
    private final JavaFileManagerImpl javaFileManager;

    private final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    private final DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
    private final List<String> options = Lists.newArrayList("-encoding", "UTF-8");

    DynamicCompilerImpl() {
        classLoader = new ClassLoaderImpl(Thread.currentThread().getContextClassLoader());

        StandardJavaFileManager manager = compiler.getStandardFileManager(diagnosticCollector, null, null);
        javaFileManager = new JavaFileManagerImpl(manager, classLoader);
    }

    /**
     * 编译Java代码
     */
    Class<?> compile(String fullClassName, String sourceCode) throws Throwable {
        checkArgument(!Strings.isNullOrEmpty(fullClassName), "类名空");
        checkArgument(!Strings.isNullOrEmpty(sourceCode), "类源代码空");

        int i = fullClassName.lastIndexOf('.');
        String packageName = i < 0 ? "" : fullClassName.substring(0, i);
        String className = i < 0 ? fullClassName : fullClassName.substring(i + 1);

        JavaFileObject javaFile = new JavaFileObjectImpl(className, sourceCode);
        javaFileManager.putFileForInput(StandardLocation.SOURCE_PATH, packageName, className + ".java", javaFile);

        Boolean success = compiler.getTask(null, javaFileManager, diagnosticCollector, options, null,
                Collections.singletonList(javaFile)).call();
        if (success == null || !success) {
            throw new IllegalStateException("编译错误: " + fullClassName + DiagnosticUtil.toString(diagnosticCollector));
        }
        return classLoader.loadClass(fullClassName);
    }

    /**
     * java文件对象. 存放源代码或编译后的字节码
     */
    private static final class JavaFileObjectImpl extends SimpleJavaFileObject {

        private final String sourceCode;
        private ByteArrayOutputStream byteCode;

        JavaFileObjectImpl(String className, String sourceCode) {
            super(ClassUtil.toURI(className + ".java"), Kind.SOURCE);
            this.sourceCode = sourceCode;
        }

        JavaFileObjectImpl(String className, Kind kind) {
            super(ClassUtil.toURI(className), kind);
            this.sourceCode = null;
        }

        byte[] getByteCode() {
            return byteCode.toByteArray();
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return sourceCode;
        }

        @Override
        public InputStream openInputStream() {
            return new ByteArrayInputStream(getByteCode());
        }

        @Override
        public OutputStream openOutputStream() {
            return byteCode = new ByteArrayOutputStream();
        }
    }

    /**
     * 类加载器
     * loadClass() call findClass(), 获取Class对象
     * findClass() call defineClass(), 把字节码变成Class对象
     */
    private static final class ClassLoaderImpl extends ClassLoader {

        private final Map<String, JavaFileObjectImpl> classes = new HashMap<String, JavaFileObjectImpl>();

        ClassLoaderImpl(ClassLoader parentClassLoader) {
            super(parentClassLoader);
        }

        void add(String fullClassName, JavaFileObjectImpl javaFile) {
            classes.put(fullClassName, javaFile);
        }

        /**
         * 只查找动态编译的类
         */
        @Override
        protected Class<?> findClass(String fullClassName) throws ClassNotFoundException {
            JavaFileObjectImpl file = classes.get(fullClassName);
            if (file == null) {
                throw new ClassNotFoundException("找不到类: " + fullClassName);
            }
            byte[] bytes = file.getByteCode();
            return defineClass(fullClassName, bytes, 0, bytes.length);
        }

        @Override
        protected synchronized Class<?> loadClass(String fullClassName, boolean resolve)
                throws ClassNotFoundException {
            return super.loadClass(fullClassName, resolve);
        }

        @Override
        public InputStream getResourceAsStream(String name) {
            if (!name.endsWith(".class")) { // name是字节码文件路径
                return null;
            }

            String fullClassName = name.substring(0, name.length() - ".class".length()).replace('/', '.');
            JavaFileObjectImpl file = classes.get(fullClassName);
            if (file != null) {
                return new ByteArrayInputStream(file.getByteCode());
            }
            return null;
        }
    }

    /**
     * java文件管理. 把java文件对象放入classLoader中
     */
    private static final class JavaFileManagerImpl extends ForwardingJavaFileManager<JavaFileManager> {

        private final ClassLoaderImpl classLoader;

        private final Map<URI, JavaFileObject> fileObjects = new HashMap<URI, JavaFileObject>();

        JavaFileManagerImpl(JavaFileManager fileManager, ClassLoaderImpl classLoader) {
            super(fileManager);
            this.classLoader = classLoader;
        }

        // 包含源代码的javaFile
        void putFileForInput(StandardLocation location, String packageName, String relativeName, JavaFileObject file) {
            fileObjects.put(uri(location, packageName, relativeName), file);
        }

        // 包含源代码的javaFile
        @Override
        public FileObject getFileForInput(Location location, String packageName, String relativeName)
                throws IOException {
            return fileObjects.get(uri(location, packageName, relativeName));
        }

        // 包含字节码的javaFile
        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String qualifiedName, JavaFileObject.Kind kind,
                                                   FileObject outputFile) throws IOException {
            JavaFileObjectImpl file = new JavaFileObjectImpl(qualifiedName, kind);
            classLoader.add(qualifiedName, file);
            return file;
        }

        @Override
        public ClassLoader getClassLoader(Location location) {
            return classLoader;
        }

        private URI uri(Location location, String packageName, String relativeName) {
            return ClassUtil.toURI(location.getName() + '/' + packageName + '/' + relativeName);
        }
    }
}
