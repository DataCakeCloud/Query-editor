package com.ushareit.query.trace.holder;

/**
 * @author wuyan
 * @date 2020/05/11
 **/
public class InfTraceContextHolder {

    private static ThreadLocal<InfWebContext> INF_WEB_CONTEXT = new InheritableThreadLocal<>();

    static{
        INF_WEB_CONTEXT.set(new InfWebContext());
    }

    public static InfWebContext get() {
        if (INF_WEB_CONTEXT.get() == null){
            INF_WEB_CONTEXT.set(new InfWebContext());
        }
        return INF_WEB_CONTEXT.get();
    }

    public static void set(InfWebContext infWebContext) {
        INF_WEB_CONTEXT.set(infWebContext);
    }

    public static void remove(){
        INF_WEB_CONTEXT.remove();
    }

    public static boolean isEmpty() {
        InfWebContext infWebContext = INF_WEB_CONTEXT.get();
        return infWebContext == null;
    }

}
