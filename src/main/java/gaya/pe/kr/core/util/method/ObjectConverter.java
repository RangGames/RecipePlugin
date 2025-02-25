package gaya.pe.kr.core.util.method;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Base64;

import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

public class ObjectConverter {
    public static String getObjectAsString(Object object) {
        try {
            ByteArrayOutputStream io = new ByteArrayOutputStream();
            BukkitObjectOutputStream os = new BukkitObjectOutputStream((OutputStream) io);
            os.writeObject(object);
            os.flush();
            byte[] serializedObject = io.toByteArray();
            return Base64.getEncoder().encodeToString(serializedObject);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Object getObject(String encodedObject) {
        try {
            byte[] serializedObject = Base64.getDecoder().decode(encodedObject);
            ByteArrayInputStream in = new ByteArrayInputStream(serializedObject);
            BukkitObjectInputStream is = new BukkitObjectInputStream((InputStream) in);
            return is.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

