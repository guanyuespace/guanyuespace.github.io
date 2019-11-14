package map;

import java.util.HashSet;
import java.util.Objects;

class UE {
    int val;
    String data;

    public UE(int val, String data) {
        this.val = val;
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UE ue = (UE) o;
        return val == ue.val &&
                Objects.equals(data, ue.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(val, data);
    }

    @Override
    public String toString() {
        return "UE{" +
                "val=" + val +
                ", data='" + data + '\'' +
                '}';
    }

    public String println() {
        return this.toString();
    }
}


public class SetTest {
    public static void main(String[] args) {
        HashSet<UE> hashSet = new HashSet<>();
        UE ue = new UE(1, "jkl");
        hashSet.add(ue);
        UE ue1 = new UE(2, "jkl");
        hashSet.add(ue1);
        ue.val = 2;
        hashSet.forEach(System.out::println);
        System.out.println(ue.hashCode() == ue1.hashCode());
        System.out.println(ue.equals(ue1));
    }
}
