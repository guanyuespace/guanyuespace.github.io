package test;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.*;

public class NTest {

    public static void main(String[] args) throws NoSuchFieldException, IllegalAccessException {
        List list = new ArrayList();
        for (int i = 0; i < 10; i++) {
            list.add(i);
        }
        System.out.println("ori:" + list.toArray() + "\t" + Arrays.toString(list.toArray()) + "\t" + list.size());

        Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        Unsafe unsafe = (Unsafe) theUnsafe.get(Unsafe.class);//or (Unsafe) theUnsafe.get(null);

        Field modCountField = AbstractList.class.getDeclaredField("modCount");
        modCountField.setAccessible(true);
        //can't get the private class : ArrayList.Itr
        //Field expectedModCountField=ArrayList.Itr.class.getDeclaredField("Itr");

        long offset = unsafe.objectFieldOffset(modCountField);
        for (Object o : list) {
            list.remove(o);//Exception
            unsafe.putInt(list, offset, (int) modCountField.get(list) - 1);//but can not back ...  and the modCount is wrong !  modCount： the modified times
        }
        System.out.println("list: " + list.toArray() + "\t" + Arrays.toString(list.toArray()) + "\t" + list.size());


        Iterator iterator = list.iterator();
        Field expectedModCountField = iterator.getClass().getDeclaredField("expectedModCount");
        long expectedModCountOffset = unsafe.objectFieldOffset(expectedModCountField);
        Field cursorField = iterator.getClass().getDeclaredField("cursor");
        cursorField.setAccessible(true);
        long cursorOffset = unsafe.objectFieldOffset(cursorField);
        Field lastRetField = iterator.getClass().getDeclaredField("lastRet");
        lastRetField.setAccessible(true);
        long lastRetOffset = unsafe.objectFieldOffset(lastRetField);
        while (iterator.hasNext()) {
            Object o = iterator.next();
            list.remove(o);
            unsafe.putInt(iterator, cursorOffset, (int) lastRetField.get(iterator));//回退指针
            unsafe.putInt(iterator, expectedModCountOffset, (int) modCountField.get(list));

        }
        System.out.println("list: " + list.toArray() + "\t" + Arrays.toString(list.toArray()) + "\t" + list.size());
    }
}
