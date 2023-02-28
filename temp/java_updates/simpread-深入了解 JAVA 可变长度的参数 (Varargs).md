> 本文由 [简悦 SimpRead](http://ksria.com/simpread/) 转码， 原文地址 https://www.cnblogs.com/uptownBoy/articles/1698335.html

 (本文转载自 csdn)

到 J2SE 1.4 为止，一直无法在 Java 程序里定义实参个数可变的方法——因为 Java 要求实参（Arguments）和形参（Parameters）的数量和类型都必须逐一匹配，而形参的数目是在定义方法时就已经固定下来了。尽管可以通过重载机制，为同一个方法提供带有不同数量的形参的版本，但是这仍然不能达到让实参数量任意变化的目的。

然而，有些方法的语义要求它们必须能接受个数可变的实参——例如著名的 main 方法，就需要能接受所有的命令行参数为实参，而命令行参数的数目，事先根本无法确定下来。

对于这个问题，传统上一般是采用 “利用一个数组来包裹要传递的实参” 的做法来应付。

## 1\. 用数组包裹实参

“用数组包裹实参” 的做法可以分成三步：首先，为这个方法定义一个数组型的参数；然后在调用时，生成一个包含了所有要传递的实参的数组；最后，把这个数组作为一个实参传递过去。

这种做法可以有效的达到 “让方法可以接受个数可变的参数” 的目的，只是调用时的形式不够简单。

J2SE 1.5 中提供了 **Varargs** 机制，允许直接定义能和多个实参相匹配的形参。从而，可以用一种更简单的方式，来传递个数可变的实参。

### **Varargs** 的含义

大体说来，“**Varargs**”是 “variable number of arguments” 的意思。有时候也被简单的称为“variable arguments”，不过因为这一种叫法没有说明是什么东西可变，所以意义稍微有点模糊。

## 2\. 定义实参个数可变的方法

只要在一个形参的 “类型” 与“参数名”之间加上三个连续的“.”（即“...”，英文里的句中省略号），就可以让它和不确定个实参相匹配。而一个带有这样的形参的方法，就是一个实参个数可变的方法。

### 清单 1：一个实参个数可变的方法

private static int sumUp(**int...** values) {
}

注意，只有最后一个形参才能被定义成 “能和不确定个实参相匹配” 的。因此，一个方法里只能有一个这样的形参。另外，如果这个方法还有其它的形参，要把它们放到前面的位置上。

编译器会在背地里把这最后一个形参转化为一个数组形参，并在编译出的 class 文件里作上一个记号，表明这是个实参个数可变的方法。

### 清单 2：实参个数可变的方法的秘密形态

private static int sumUp(**int[]** values) {
}

由于存在着这样的转化，所以不能再为这个类定义一个和转化后的方法签名一致的方法。

### 清单 3：会导致编译错误的组合

private static int sumUp(**int...** values) {
}
private static int sumUp(**int[]** values) {
}

## 3\. 调用实参个数可变的方法

只要把要传递的实参逐一写到相应的位置上，就可以调用一个实参个数可变的方法。不需要其它的步骤。

### 清单 4：可以传递若干个实参

sumUp(**1, 3, 5, 7**);

在背地里，编译器会把这种调用过程转化为用 “数组包裹实参” 的形式：

### 清单 5：偷偷出现的数组创建

sumUp(**new int[]{1, 2, 3, 4}**);

另外，这里说的 “不确定个” 也包括零个，所以这样的调用也是合乎情理的：

### 清单 6：也可以传递零个实参

sumUp**()**;

这种调用方法被编译器秘密转化之后的效果，则等同于这样：

### 清单 7：零实参对应空数组

sumUp(new int[]**{}**);

注意这时传递过去的是一个空数组，而不是 null。这样就可以采取统一的形式来处理，而不必检测到底属于哪种情况。

## 4\. 处理个数可变的实参

处理个数可变的实参的办法，和处理数组实参的办法基本相同。所有的实参，都被保存到一个和形参同名的数组里。根据实际的需要，把这个数组里的元素读出之后，要蒸要煮，就可以随意了。

### 清单 8：处理收到的实参们

private static int sumUp(int... **values**) {
 int sum = 0;
 for (int i = 0; i < **values**.length; i++) {
  sum += **values**[i];
 }
 return sum;
}

## 5\. 转发个数可变的实参

有时候，在接受了一组个数可变的实参之后，还要把它们传递给另一个实参个数可变的方法。因为编码时无法知道接受来的这一组实参的数目，所以 “把它们逐一写到该出现的位置上去” 的做法并不可行。不过，这并不意味着这是个不可完成的任务，因为还有另外一种办法，可以用来调用实参个数可变的方法。

在 J2SE 1.5 的编译器的眼中，实参个数可变的方法是最后带了一个数组形参的方法的特例。因此，事先把整组要传递的实参放到一个数组里，然后把这个数组作为最后一个实参，传递给一个实参个数可变的方法，不会造成任何错误。借助这一特性，就可以顺利的完成转发了。

### 清单 9：转发收到的实参们

public class PrintfSample {
  public static void main(String[] args) {
    printOut("Pi:%f E:%f\n", Math.PI, Math.E);
  }
  private static void printOut(String format, **Object... args**) {
    System.out.printf(format, **args**);
 }
}

## 6\. 是数组？不是数组？

尽管在背地里，编译器会把能匹配不确定个实参的形参，转化为数组形参；而且也可以用数组包了实参，再传递给实参个数可变的方法；但是，这并不表示 “能匹配不确定个实参的形参” 和“数组形参”完全没有差异。

一个明显的差异是，如果按照调用实参个数可变的方法的形式，来调用一个最后一个形参是数组形参的方法，只会导致一个 “cannot be applied to” 的编译错误。

### 清单 10：一个 “cannot be applied to” 的编译错误

private static void testOverloading(**int[]** i) {
System.out.println("A");
}
public static void main(String[] args) {
testOverloading(**1, 2, 3**);// 编译出错
}

由于这一原因，不能在调用只支持用数组包裹实参的方法的时候（例如在不是专门为 J2SE 1.5 设计第三方类库中遗留的那些），直接采用这种简明的调用方式。

如果不能修改原来的类，为要调用的方法增加参数个数可变的版本，而又想采用这种简明的调用方式，那么可以借助 “引入外加函数（Introduce Foreign Method）” 和“引入本地扩展（Intoduce Local Extension）”的重构手法来近似的达到目的。

## 7\. 当个数可变的实参遇到泛型

J2SE 1.5 中新增了 “泛型” 的机制，可以在一定条件下把一个类型参数化。例如，可以在编写一个类的时候，把一个方法的形参的类型用一个标识符（如 T）来代表，至于这个标识符到底表示什么类型，则在生成这个类的实例的时候再行指定。这一机制可以用来提供更充分的代码重用和更严格的编译时类型检查。

不过泛型机制却不能和个数可变的形参配合使用。如果把一个能和不确定个实参相匹配的形参的类型，用一个标识符来代表，那么编译器会给出一个 “generic array creation” 的错误。

### 清单 11：当 **Varargs** 遇上泛型

private static void testVarargs(**T**... args) {// 编译出错
}

造成这个现象的原因在于 J2SE 1.5 中的泛型机制的一个内在约束——不能拿用标识符来代表的类型来创建这一类型的实例。在出现支持没有了这个约束的 Java 版本之前，对于这个问题，基本没有太好的解决办法。

不过，传统的 “用数组包裹” 的做法，并不受这个约束的限制。

### 清单 12：可以编译的变通做法

private static void testVarargs(**T**[] args) {
 for (int i = 0; i < args.length; i++) {
  System.out.println(args[i]);
 }
}

## 8\. 重载中的选择问题

Java 支持 “重载” 的机制，允许在同一个类拥有许多只有形参列表不同的方法。然后，由编译器根据调用时的实参来选择到底要执行哪一个方法。

传统上的选择，基本是依照 “特殊者优先” 的原则来进行。一个方法的特殊程度，取决于为了让它顺利运行而需要满足的条件的数目，需要条件越多的越特殊。

在引入 **Varargs** 机制之后，这一原则仍然适用，只是要考虑的问题丰富了一些——传统上，一个重载方法的各个版本之中，只有形参数量与实参数量正好一致的那些有被进一步考虑的资格。但是 **Varargs** 机制引入之后，完全可以出现两个版本都能匹配，在其它方面也别无二致，只是一个实参个数固定，而一个实参个数可变的情况。

遇到这种情况时，所用的判定规则是 “实参个数固定的版本优先于实参个数可变的版本”。

### 清单 13：实参个数固定的版本优先

如果在编译器看来，同时有多个方法具有相同的优先权，它就会陷入无法就到底调用哪个方法作出一个选择的状态。在这样的时候，它就会产生一个 “reference to 被调用的方法名 is ambiguous” 的编译错误，并耐心的等候作了一些修改，足以免除它的迷惑的新源代码的到来。

在引入了 **Varargs** 机制之后，这种可能导致迷惑的情况，又增加了一些。例如现在可能会有两个版本都能匹配，在其它方面也如出一辙，而且都是实参个数可变的冲突发生。

public class OverloadingSampleA {
 public static void main(String[] args) {
  testOverloading(1);// 打印出 A
  testOverloading(1, 2);// 打印出 B
  testOverloading(1, 2, 3);// 打印出 C
 }
 private static void testOverloading(int i) {
  System.out.println("A");
 }
 private static void testOverloading(int i, int j) {
  System.out.println("B");
 }
 private static void testOverloading(int i, int... more) {
  System.out.println("C");
 }
}
如果在编译器看来，同时有多个方法具有相同的优先权，它就会陷入无法就到底调用哪个方法作出一个选择的状态。在这样的时候，它就会产生一个 “reference to 被调用的方法名 is ambiguous” 的编译错误，并耐心的等候作了一些修改，足以免除它的迷惑的新源代码的到来。

在引入了 Varargs 机制之后，这种可能导致迷惑的情况，又增加了一些。例如现在可能会有两个版本都能匹配，在其它方面也如出一辙，而且都是实参个数可变的冲突发生。

### 清单 14：左右都不是，为难了编译器

public class OverloadingSampleB {
 public static void main(String[] args) {
  testOverloading(1, 2, 3);// 编译出错
 }
 private static void testOverloading(**Object... args**) {
 }
 private static void testOverloading(**Object o, Object... args**) {
 }
}

另外，因为 J2SE 1.5 中有 “Autoboxing/Auto-Unboxing” 机制的存在，所以还可能发生两个版本都能匹配，而且都是实参个数可变，其它方面也一模一样，只是一个能接受的实参是基本类型，而另一个能接受的实参是包裹类的冲突发生。

### 清单 15：Autoboxing/Auto-Unboxing 带来的新问题

public class OverloadingSampleC {
 public static void main(String[] args) {
  /* 编译出错 */
  testOverloading(1, 2);
  /* 还是编译出错 */
  testOverloading(new Integer(1), new Integer(2));
 }
 private static void testOverloading(int... args) {
 }
 private static void testOverloading(Integer... args) {
 }
}

## 9\. 归纳总结

和 “用数组包裹” 的做法相比，真正的实参个数可变的方法，在调用时传递参数的操作更为简单，含义也更为清楚。不过，这一机制也有它自身的局限，并不是一个完美无缺的解决方案。