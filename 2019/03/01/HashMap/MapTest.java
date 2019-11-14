package map;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;


public class MapTest {
    public static void main(String[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {


        HashMap<UE, String> hashMap = new HashMap<>();
        UE ue = new UE(1, "jkl");
        UE ue1 = new UE(2, "jkl");
        hashMap.put(ue, "jkl");
        ue.val = 2;//but the place in which it has been stored doesn't change....   (the entry hash value will not change)
        hashMap.forEach((i, j) -> {
            System.out.println(i.toString() + "\t" + j);
        });
        System.out.println(ue.hashCode() == ue1.hashCode());
        System.out.println(ue.equals(ue1));
        hashMap.put(ue1, "jkl");//ok   because stored value table[hash&length-1]=null ---> so will insert into the true place.
        hashMap.forEach((i, j) -> {
            System.out.println(i.toString() + "\t" + j);
        });

      /*  HashMap hashMap = new HashMap(9);
        Method capacityMethod = HashMap.class.getDeclaredMethod("capacity");
        capacityMethod.setAccessible(true);
        System.out.println(capacityMethod.invoke(hashMap));
        for (int i = 0; i < 13; i++) {
            hashMap.put(i, 100 + i);
        }

        hashMap.replaceAll((i, j) -> i);
        printResult(hashMap);
        System.out.println(capacityMethod.invoke(hashMap));//13>16*0.75   --->  32

        for (int i = 0; i < 13; i++) {
            hashMap.merge(i, 100, (oldVale, value) -> (int) oldVale + (int) value);
        }
        printResult(hashMap);

        for (int i = 0; i < 13; i++) {
            hashMap.compute(i, (key, value) -> {
                return (int) value + (int) key;
            });
        }
        printResult(hashMap);*/
    }

    private static void printResult(Map<Integer, Integer> map) {
        for (Map.Entry<Integer, Integer> e : map.entrySet()) {
            System.out.println("key:" + e.getKey() + "\t" + "value:" + e.getValue());
        }
    }

}
