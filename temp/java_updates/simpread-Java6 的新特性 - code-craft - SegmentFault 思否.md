> 本文由 [简悦 SimpRead](http://ksria.com/simpread/) 转码， 原文地址 https://segmentfault.com/a/1190000004417536

> Java 语言特性系列

- [Java5 的新特性](https://segmentfault.com/a/1190000004417288)
- [Java6 的新特性](https://segmentfault.com/a/1190000004417536)
- [Java7 的新特性](https://segmentfault.com/a/1190000004417830)
- [Java8 的新特性](https://segmentfault.com/a/1190000004419611)
- [Java9 的新特性](https://segmentfault.com/a/1190000013620826)
- [Java10 的新特性](https://segmentfault.com/a/1190000014076481)
- [Java11 的新特性](https://segmentfault.com/a/1190000016527932)
- [Java12 的新特性](https://segmentfault.com/a/1190000018588495)
- <a>Java13 的新特性</a>

## 序

本文梳理了下 java6 的新特性，相对于 java5 而言，java6 的特性显得少些，分量也不那么重，相当于 java5 是 windows xp，java6 有点像 vista。

## 特性列表

- JSR223 脚本引擎
- JSR199--Java Compiler API
- JSR269--Pluggable Annotation Processing API
- 支持 JDBC4.0 规范
- JAX-WS 2.0 规范

### 1、JSR223 脚本引擎

Scripting for the Java Platform

####  基本使用

```java
public class BasicScripting {
    public void greet() throws ScriptException {
        ScriptEngineManager manager = new ScriptEngineManager();
        //支持通过名称、文件扩展名、MIMEtype查找
        ScriptEngine engine = manager.getEngineByName("JavaScript");
//        engine = manager.getEngineByExtension("js");
//        engine = manager.getEngineByMimeType("text/javascript");
        if (engine == null) {
            throw new RuntimeException("找不到JavaScript语言执行引擎。");
        }
        engine.eval("println('Hello!');");
    }
    public static void main(String[] args) {
        try {
            new BasicScripting().greet();
        } catch (ScriptException ex) {
            Logger.getLogger(BasicScripting.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
```

####  绑定上下文
>定义变量所绑定的上下文

```java
public class ScriptContextBindings extends JsScriptRunner {
    public void scriptContextBindings() throws ScriptException {
        ScriptEngine engine = getJavaScriptEngine();
        ScriptContext context = engine.getContext();
        Bindings bindings1 = engine.createBindings();
        bindings1.put("name", "Alex");
        context.setBindings(bindings1, ScriptContext.GLOBAL_SCOPE);
        Bindings bindings2 = engine.createBindings();
        bindings2.put("name", "Bob");
        context.setBindings(bindings2, ScriptContext.ENGINE_SCOPE);
        engine.eval("println(name);");
    }
    public void useScriptContextValues() throws ScriptException {
        ScriptEngine engine = getJavaScriptEngine();
        ScriptContext context = engine.getContext();
        Bindings bindings = context.getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.put("name", "Alex");
        engine.eval("println(name);");
    }
    public void attributeInBindings() throws ScriptException {
        ScriptEngine engine = getJavaScriptEngine();
        ScriptContext context = engine.getContext();
        context.setAttribute("name", "Alex", ScriptContext.GLOBAL_SCOPE);
        engine.eval("println(name);");
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws ScriptException {
        ScriptContextBindings scb = new ScriptContextBindings();
        scb.scriptContextBindings();
        scb.useScriptContextValues();
        scb.attributeInBindings();
    }
}
```

### 2、JSR199--Java Compiler API
> 运行时编译Java源文件
> 参见[Java_Compiler]()

```java
public class JavaCompilerAPICompiler {
    public void compile(Path src, Path output) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
            Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjects(src.toFile());
            Iterable<String> options = Arrays.asList("-d", output.toString());
            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, null, options, null, compilationUnits);
            boolean result = task.call();
        }
    }
}
```

### 3、JSR269--Pluggable Annotation Processing API
>插件化注解，AnnotationProcessor的使用
>编译时注解，通过注解对类操作<!--例如：@builder注解--根据People类生成PeopleBuilder减少代码量 -->

一部分是进行注解处理的 javax.annotation.processing，另一部分是对程序的静态结构进行建模的 javax.lang.model

### 4、其他

####  支持 JDBC4.0 规范
####  JAX-WS 2.0 规范 (`包括JAXB 2.0`)
####  轻量级 HttpServer

## 参考

-  [Java 6 Features and Enhancements](http://www.oracle.com/technetwork/java/javase/features-141434.html)
-  [Java Platform, Standard Edition Differences between 5.0 fcs and 6 Beta](http://docs.oracle.com/cd/E17802_01/j2se/javase/6/jcp/beta/apidiffs/index-diffs.html)</span></span></span>
