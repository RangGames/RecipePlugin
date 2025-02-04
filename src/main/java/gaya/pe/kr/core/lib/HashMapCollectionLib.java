package gaya.pe.kr.core.lib;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HashMapCollectionLib<K, V>
implements Serializable {
    HashMap<K, V> hashMap = new HashMap();

    public void putData(K key, V value) {
        this.hashMap.put(key, value);
    }

    public void removeKey(K key) {
        this.hashMap.remove(key);
    }

    public void removeValue(V value) {
        HashMap<Object, Object> cloneHash = new HashMap<Object, Object>(this.hashMap);
        cloneHash.forEach((k, v) -> {
            if (v.equals(value)) {
                this.hashMap.remove(k);
            }
        });
    }

    public boolean existKey(K key) {
        return this.hashMap.containsKey(key);
    }

    public boolean existValue(V value) {
        return this.hashMap.containsValue(value);
    }

    public V getValue(K key) {
        return this.hashMap.get(key);
    }

    public int getSize() {
        return this.hashMap.size();
    }

    public List<V> getValues() {
        return new ArrayList<V>(this.hashMap.values());
    }

    public List<K> getKeys() {
        return new ArrayList<K>(this.hashMap.keySet());
    }
}

