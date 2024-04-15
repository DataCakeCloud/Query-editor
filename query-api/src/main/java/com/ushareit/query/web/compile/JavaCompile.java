package com.ushareit.query.web.compile;

import lombok.extern.slf4j.Slf4j;

import javax.tools.*;
import java.net.URI;
import java.util.Arrays;

/**
 * @author: licg
 * @create: 2020-12-08
 **/
@Slf4j
public class JavaCompile implements CompileBase {

    @Override
    public void compile(String className, String classBody, String classPath, String classTargetDir) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        StringSourceJavaObject sourceObject = new StringSourceJavaObject(className, classBody);
        Iterable<? extends JavaFileObject> fileObjects = Arrays.asList(sourceObject);
        Iterable<String> options = Arrays.asList("-classpath", classPath, "-d", classTargetDir);
        log.info(String.format("-classpath %s -d %s", classPath, classTargetDir));
        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, null, options, null, fileObjects);
        boolean result = task.call();
        if (result) {
            log.info(String.format("编译成功 %s", className));
        } else {
            throw new RuntimeException(String.format("编译失败 %s,请检查udf代码！", className));
        }
    }

    static class StringSourceJavaObject extends SimpleJavaFileObject {
        private String content;

        public StringSourceJavaObject(String name, String content) {
            super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.content = content;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return content;
        }
    }
}
