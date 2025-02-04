package gaya.pe.kr.core.lib;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ListCollectionLib<T>
implements Serializable {
    List<T> list = new ArrayList<T>();

    public void addData(T targetData) {
        this.list.add(targetData);
    }

    public void removeData(T targetData) {
        this.list.remove(targetData);
    }

    public boolean existData(T targetData) {
        return this.list.contains(targetData);
    }

    public List<T> getList() {
        return this.list;
    }

    public boolean isEmpty() {
        return this.list.isEmpty();
    }
}

