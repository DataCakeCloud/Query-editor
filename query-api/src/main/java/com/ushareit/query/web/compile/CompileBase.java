package com.ushareit.query.web.compile;

/**
 * @author: licg
 * @create: 2020-12-08
 **/
public interface CompileBase {
    /**
     * 编译代码
     *
     * @param className
     * @param classBody
     * @param classPath
     * @param classTargetDir
     */
    void compile(String className, String classBody, String classPath, String classTargetDir);
}
