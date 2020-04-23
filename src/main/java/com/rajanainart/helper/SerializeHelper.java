package com.rajanainart.helper;

import java.io.*;
import java.util.Base64;

public final class SerializeHelper {

    public static String serialize(Object data) throws IOException {
        ByteArrayOutputStream byteStream   = new ByteArrayOutputStream();
        ObjectOutputStream    objectStream = new ObjectOutputStream(byteStream);
        objectStream.writeObject(data);
        objectStream.close();

        byte[] bytes  = byteStream.toByteArray();
        String result = Base64.getEncoder().encodeToString(bytes);
        return result;
    }

    public static <T> T deserialize(String serializedObject) throws IOException, ClassNotFoundException {
        byte[] bytes = Base64.getDecoder().decode(serializedObject);
        ByteArrayInputStream byteStream   = new ByteArrayInputStream(bytes);
        ObjectInputStream    objectStream = new ObjectInputStream(byteStream);
        return (T)objectStream.readObject();
    }
}
