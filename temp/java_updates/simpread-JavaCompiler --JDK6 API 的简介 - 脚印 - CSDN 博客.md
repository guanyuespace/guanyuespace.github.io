# JavaCompiler
> [原文地址](https://blog.csdn.net/pkuyjxu/article/details/8609457)

## JavaCompiler JDK6 API 的简介

在非常多 Java 应用中需要在程式中调用 Java 编译器来编译和运行。但在早期的版本中 (Java SE5 及以前版本) 中只能通过 tools.jar 中的 com.sun.tools.javac 包来调用 Java 编译器，但由于 tools.jar 不是标准的 Java 库，在使用时必须要设置这个 jar 的路径。而在 Java SE6 中为我们提供了标准的包来操作 Java 编译器，这就是 javax.tools 包。使用这个包，我们能不用将 jar 文件路径添加到 classpath 中了。
<!-- JavaCompiler编译生成class文件 -->
<!-- 与Java Class Loader之间的关系，自定义ClassLoader加载class文件，Class对象反射操作, ok-->


## 一、使用JavaCompiler 接口来编译 Java 源程式

使用 Java API 来编译 Java 源程式有非常多方法，目前让我们来看一种最简单的方法，通过 JavaCompiler 进行编译。

我们能通过 ToolProvider 类的静态方法 getSystemJavaCompiler 来得到一个 JavaCompiler 接口的实例。

```java
JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
```

JavaCompiler 中最核心的方法是 run。通过这个方法能编译 java 源程式。这个方法有 3 个固定参数和 1 个可变参数 (可变参数是从 Jave SE5 开始提供的一个新的参数类型，用 type… argu 表示)。前 3 个参数分别用来为 java 编译器提供参数、得到 Java 编译器的输出信息及接收编译器的错误信息，后面的可变参数能传入一个或多个 Java 源程式文件。如果 run 编译成功，返回 0。

```java
int run(InputStream in, OutputStream out, OutputStream err, String... arguments);
```

如果前 3 个参数传入的是 null，那么 run 方法将以标准的输入、输出代替，即 System.in、System.out 和 System.err。如果我们要编译一个 test.java 文件，并将使用标准输入输出，run 的使用方法如下：

```java
int results = tool.run(null, null, null, "test.java");
```

下面是使用 JavaCompiler 的完整代码：

```java
import java.io.*;
import javax.tools.*;

public class test_compilerapi{
    public static void main(String args[]) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        int results = compiler.run(null, null, null, "test.java");
        System.out.println((results == 0)?" 编译成功 ":" 编译失败 ");

        // 在程式中运行 test
        Runtime run = Runtime.getRuntime();
        Process p = run.exec("java test");
        BufferedInputStream in = new BufferedInputStream(p.getInputStream());
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String s;
        while ((s = br.readLine()) != null)
            System.out.println(s);
    }
}

public class test{
    public static void main(String[] args) throws Exception{
        System.out.System.out.println("Hello, this is in test#main.");
    }
}
```

### 编译成功的输出结果：

- 编译成功
JavaCompiler 测试成功

- 编译失败的输出结果：
test.java:9: 未找到符号
符号：方法 printlnln(java.lang.String)
位置：类 java.io.PrintStream
System.out.printlnln("JavaCompiler 测试成功！");
^
1 错误
编译失败


## 二、使用 StandardJavaFileManager 编译 Java 源程式

在第一部分我们讨论调用 java 编译器的最容易的方法。这种方法能非常好地工作，但他确不能更有效地得到我们所需要的信息，如标准的输入、输出信息。而在 Java SE6 中最佳的方法是使用 `StandardJavaFileManager` 类。这个类能非常好地控制输入、输出，并且能通过 `DiagnosticListener` 得到诊断信息，而 `DiagnosticCollector` 类就是 `listener` 的实现。

使用 `StandardJavaFileManager` 需要两步。首先建立一个 `DiagnosticCollector` 实例及通过 `JavaCompiler` 的 `getStandardFileManager`() 方法得到一个 `StandardFileManager` 对象。最后通过 `CompilationTask` 中的 `call` 方法编译源程式。

在使用这种方法调用 Java 编译时最复杂的方法就是 `getTask`，下面让我们讨论一下 `getTask` 方法。这个方法有如下所示的 6 个参数。
```java
getTask(Writer out,//用于输出错误的流，默认是 System.err。
JavaFileManager fileManager,//标准的文件管理。
DiagnosticListener<? super JavaFileObject> diagnosticListener,//编译器的默认行为
Iterable<String> options,//编译器的选项
Iterable<String> classes,//参与编译的class文件，拥有调用关系的class
Iterable<? extends JavaFileObject> compilationUnits//不能为 null，因为这个对象保存了你想编译的 Java 文件。
)
```

在使用完 `getTask` 后，需要通过 `StandardJavaFileManager` 的 `getJavaFileObjectsFromFiles` 或 `getJavaFileObjectsFromStrings` 方法得到 `compilationUnits` 对象。调用这两个方法的方式如下：

```java
Iterable<? extends JavaFileObject> getJavaFileObjectsFromFiles(
Iterable<? extends File> files)
Iterable<? extends JavaFileObject> getJavaFileObjectsFromStrings(
Iterable<String> names)

String[] filenames = …;
Iterable<? extends JavaFileObject> compilationUnits =
fileManager.getJavaFileObjectsFromFiles(Arrays.asList(filenames));

JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager,
diagnostics, options, null, compilationUnits);
```

最后需要关闭 `fileManager.close();`

### Demo

```java
import java.io.*;
import java.util.*;
import javax.tools.*;

public class test_compilerapi{
    private static void compilejava() throws Exception{
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        // 建立 DiagnosticCollector 对象
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
        // 建立用于保存被编译文件名的对象
        // 每个文件被保存在一个从 JavaFileObject 继承的类中
        Iterable<? extends JavaFileObject> compilationUnits = fileManager        .getJavaFileObjectsFromStrings(Arrays asList("test3.java"));
        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, null, null, compilationUnits);
        // 编译源程式
        boolean success = task.call();
        fileManager.close();
        System.out.println((success)?" 编译成功 ":" 编译失败 ");
    }
    public static void main(String args[]) throws Exception{
        compilejava();
    }

}
```

#### 如果想得到具体的编译错误，能对 Diagnostics 进行扫描，代码如下：

```java
for (Diagnostic diagnostic : diagnostics.getDiagnostics())
  System.out.printf("Code: %s%n" + "Kind: %s%n" + "Position: %s%n" + "Start Position: %s%n" + "End Position: %s%n" + "Source: %s%n" + "Message: %s%n", diagnostic.getCode(), diagnostic.getKind(), diagnostic.getPosition(), diagnostic.getStartPosition(), diagnostic.getEndPosition(), diagnostic.getSource(), diagnostic.getMessage(null));
```

#### 被编译的 test.java 代码如下：

```java
public class test{
  public static void main(String[] args) throws Exception{
    aa; // 错误语句
    System.out.println("JavaCompiler 测试成功！");
  }
}
```

在这段代码中多写了个 aa，得到的编译错误为：

```
Code: compiler.err.not.stmt
Kind: ERROR
Position: 89
Start Position: 89
End Position: 89
Source: test.java
Message: test.java:5: 不是语句
Success: false
```

#### OPTION:自定义目标生成路径

通过 `JavaCompiler` 进行编译都是在当前目录下生成.class 文件，而使用编译选项能改动这个默认目录。编译选项是个元素为 `String` 类型的 `Iterable` 集合。如我们能使用如下代码在 D 盘根目录下生成.class 文件。

```java
Iterable<String> options = Arrays.asList("-d", "d:");
JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null, compilationUnits);
```

在上面的例子中 `options` 处的参数为 `null`，而要传递编译器的参数，就需要将 `options` 传入。

有时我们编译一个 Java 源程式文件，而这个源程式文件需要另几个 Java 文件，而这些 Java 文件又在另外一个目录，那么这就需要为编译器指定这些文件所在的目录。

```java
Iterable<String> options = Arrays.asList("-sourcepath", "d:src");
```

上面的代码指定的被编译 Java 文件所依赖的源文件所在的目录。

## 三、从内存中编译

`JavaCompiler` 不仅能编译硬盘上的 Java 文件，而且还能**编译内存中的 Java 代码**，然后使用 `reflection` 来运行他们。我们能编写一个 `JavaSourceFromString` 类，通过这个类能输入 Java 原始码。一但建立这个对象，你能向其中输入任意的 Java 代码，然后编译和运行，而且无需向硬盘上写.class 文件。

```java
import java.lang.reflect.*;
import java.io.*;
import javax.tools.*;
import javax.tools.JavaCompiler.CompilationTask;
import java.util.*;
import java.net.*;

public class test_compilerapi
{
  private static void compilerJava() throws Exception{
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
    // 定义一个 StringWriter 类，用于写 Java 程式
    StringWriter writer = new StringWriter();
    PrintWriter out = new PrintWriter(writer);
    // 开始写 Java 程式
    out.println("public class HelloWorld {");
    out.println("public static void main(String args[]) {");
    out.println("System.out.println("Hello, World");");
    out.println("}");
    out.println("}");
    out.close();
    // 为这段代码取个名子：HelloWorld，以便以后使用 reflection 调用
    JavaFileObject file = new JavaSourceFromString("HelloWorld", writer.toString());
    Iterable<? extends JavaFileObject> compilationUnits = Arrays.asList(file);
    JavaCompiler.CompilationTask task = compiler.getTask(null, null, diagnostics, null, null, compilationUnits);
    boolean success = task.call();
    System.out.println("Success:" + success);
    // 如果成功，通过 reflection 执行这段 Java 程式
    if (success){
      System.out.println("----- 输出 -----");
      Class.forName("HelloWorld").getDeclaredMethod("main", new Class[]{String[].class }).invoke(null, new Object[]{null});
      System.out.println("----- 输出 -----");
    }
  }
  public static void main(String args[]) throws Exception{
    compilerJava();
  }
}
// 用于传递源程式的 JavaSourceFromString 类
class JavaSourceFromString extends SimpleJavaFileObject{
  final String code;
  JavaSourceFromString(String name, String code){
    super(URI.create("string:///" + name.replace(’.’, ’/’)+ Kind.SOURCE.extension), Kind.SOURCE);
    this.code = code;
  }
  @Override
  public CharSequence getCharContent(boolean ignoreEncodingErrors){
    return code;
  }
}
```

### 简要

```java
public interface JavaCompiler extends Tool, OptionChecker {...}
```

从程序中调用 Java™ 编程语言编译器的接口。

编译过程中，编译器可能生成诊断信息（例如，错误消息）。如果提供了诊断侦听器，那么诊断信息将被提供给该侦听器。如果没有提供侦听器，那么除非另行指定，否则诊断信息将被格式化为未指定的格式，并被写入到默认输出 System.err。即使提供了诊断侦听器，某些诊断信息也可能不适合 Diagnostic，并将被写入到默认输出。

编译器工具具有关联的标准文件管理器，此文件管理器是工具本地的（或内置的）。可以通过调用 `getStandardFileManager` 获取该标准文件管理器。

- 只要满足下面方法详细描述中的任意附加需求，编译器工具就必须与文件管理器一起运行。如果没有提供文件管理器，则编译器工具将使用标准文件管理器，比如 getStandardFileManager 返回的标准文件管理器。

实现此接口的实例必须符合 Java Language Specification 并遵照 Java Virtual Machine 规范生成类文件。Tool 接口中定义了这些规范的版本。此外，支持 SourceVersion.RELEASE_6 或更高版本的此接口的实例还必须支持注释处理。

编译器依赖于两种服务：诊断侦听器和文件管理器。虽然此包中的大多数类和接口都定义了编译器（和一般工具）的 API，但最好不要在应用程序中使用接口 DiagnosticListener、JavaFileManager、FileObject 和 JavaFileObject。应该实现这些接口，用于为编译器提供自定义服务，从而定义编译器的 SPI。

此包中有很多类和接口，它们被设计用于简化 SPI 的实现，以自定义编译器行为：StandardJavaFileManager 实现此接口的每个编译器都提供一个标准的文件管理器，以便在常规文件上进行操作。StandardJavaFileManager 接口定义了从常规文件创建文件对象的其他方法。

标准文件管理器有两个用途：
- 自定义编译器如何读写文件的基本构建块
- 在多个编译任务之间共享

重新使用文件管理器可能会减少扫描文件系统和读取 jar 文件的开销。标准文件管理器必须与多个顺序编译共同工作，尽管这样做并不能减少开销，下例是建议的编码模式：

```java
Files[] files1 = ...; // input for firstcompilation task
Files[] files2 = ...; // input for secondcompilation task
JavaCompiler compiler =ToolProvider.getSystemJavaCompiler();
StandardJavaFileManager fileManager =compiler.getStandardFileManager(null, null, null);
Iterable<? extends JavaFileObject>compilationUnits1 =fileManager.getJavaFileObjectsFromFiles(Arrays.asList(files1));

//compile first file
compiler.getTask(null, fileManager, null,null, null, compilationUnits1).call();

Iterable<? extends JavaFileObject>compilationUnits2 =fileManager.getJavaFileObjects(files2); //use alternative method

// reuse the same file manager to allowcaching of jar files
compiler.getTask(null, fileManager, null,null, null, compilationUnits2).call();
fileManager.close();
```

DiagnosticCollector
用于将诊断信息收集在一个列表中，例如：
```java
Iterable<? extendsJavaFileObject> compilationUnits = ...;
JavaCompiler compiler =ToolProvider.getSystemJavaCompiler();
DiagnosticCollector<JavaFileObject>diagnostics = new DiagnosticCollector<JavaFileObject>();
StandardJavaFileManager fileManager =compiler.getStandardFileManager(diagnostics, null, null);
compiler.getTask(null, fileManager,diagnostics, null, null, compilationUnits).call();
for (Diagnostic diagnostic:diagnostics.getDiagnostics()){//诊断信息打印
  System.out.format("Error on line %d in%d%n", diagnostic.getLineNumber(), diagnostic.getSource().toUri());
}
fileManager.close();
```

ForwardingJavaFileManager、ForwardingFileObject 和 ForwardingJavaFileObject子类化不可用于重写标准文件管理器的行为，因为标准文件管理器是通过调用编译器上的方法创建的，而不是通过调用构造方法创建的。应该使用转发（或委托）。允许自定义行为时，这些类使得将多个调用转发到给定文件管理器或文件对象变得容易。例如，考虑如何将所有的调用记录到JavaFileManager.flush()：

```java
final Logger logger = ...;
Iterable<? extends JavaFileObject>compilationUnits = ...;
JavaCompiler compiler =ToolProvider.getSystemJavaCompiler();
StandardJavaFileManager stdFileManager =compiler.getStandardFileManager(null, null, null);
JavaFileManager fileManager = new ForwardingJavaFileManager(stdFileManager){
  public void flush() {
    logger.entering(StandardJavaFileManager.class.getName(),"flush");
    super.flush();
    logger.exiting(StandardJavaFileManager.class.getName(),"flush");
  }
};
compiler.getTask(null, fileManager, null,null, null, compilationUnits).call();
```

SimpleJavaFileObject此类提供基本文件对象实现，该实现可用作创建文件对象的构建块。例如，下例显示了如何定义表示存储在字符串中的源代码的文件对象：

```java
/**
 * A file object used to represent sourcecoming from a string.
 */
public class JavaSourceFromString extends SimpleJavaFileObject {
  /**
   * The source code of this "file".
   */
   final String code;
   JavaSourceFromString(String name, Stringcode) {
     super(URI.create("string:///" +name.replace('.','/') + Kind.SOURCE.extension),Kind.SOURCE);
     this.code = code;
   }

  @Override
  public CharSequence getCharContent(booleanignoreEncodingErrors) {
    return code;
  }
}
```



#### 另请参见：
>从以下版本开始：1.6
>DiagnosticListener, Diagnostic, JavaFileManager


嵌套类摘要

```java
static interface JavaCompiler.CompilationTask;  //表示编译任务的 future 的接口。
```


方法摘要
```java
StandardJavaFileManager getStandardFileManager(DiagnosticListener<? super JavaFileObject>diagnosticListener, Locale locale, Charset charset) //为此工具获取一个标准文件管理器实现的新实例。
JavaCompiler.CompilationTask getTask(Writer out,JavaFileManager fileManager, DiagnosticListener<? super JavaFileObject>diagnosticListener, Iterable<String> options, Iterable<String>classes, Iterable<? extends JavaFileObject> compilationUnits)//使用给定组件和参数创建编译任务的 future。
```
