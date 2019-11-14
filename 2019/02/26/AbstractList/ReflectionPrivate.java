package test;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;


public class ReflectionPrivate {
    public static void main(String[] args) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        List list = new ArrayList(10);
        for (int i = 0; i < 10; i++) {
            list.add(i);
        }
        Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        Unsafe unsafe = (Unsafe) theUnsafe.get(Unsafe.class);//or (Unsafe) theUnsafe.get(null);

        System.out.println("list: " + Arrays.toString(list.toArray()) + "  size：" + list.size());
        Class ItrClazz = Class.forName("java.util.ArrayList$Itr");
        Field expectedModCountField = ItrClazz.getDeclaredField("expectedModCount");
        expectedModCountField.setAccessible(true);
        long expectedModCountOffset = unsafe.objectFieldOffset(expectedModCountField);

        Iterator iterator = list.iterator();
        System.out.println(expectedModCountField.get(iterator));
        unsafe.putInt(iterator, expectedModCountOffset, 2);
        System.out.println(expectedModCountField.get(iterator));
    }
}
