package me.skizzme.easyjson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.skizzme.easyjson.annotation.JsonField;
import me.skizzme.easyjson.annotation.JsonObjectField;
import me.skizzme.easyjson.annotation.JsonPropertyField;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

public class EasyJson {

    public static Gson gson = new GsonBuilder().create();

    public static JsonObject serialize(Object obj) {
        JsonObject object = new JsonObject();

        Class<?> objClass = obj.getClass();
        Field[] fields = objClass.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            Annotation[] annotations = field.getDeclaredAnnotations();

            Object value = null;
            try {
                value = field.get(obj);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

            for (Annotation a : annotations) {
                if (a instanceof JsonField annotation) {
                    String name = annotation.name();
                    Class<?> serializer = annotation.serializer();
                    try {
                        object.add(name, (JsonElement) serializer.getMethod("serialize", Object.class).invoke(serializer.getConstructors()[0].newInstance(), value));
                    } catch (IllegalAccessException e) {
                        System.err.println("Could not access \"serialize\" method for class \"" + objClass.getName() + "\" (" + e.getStackTrace()[0] + ")");
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    } catch (NoSuchMethodException e) {
                        System.err.println("\"serialize\" method for class \"" + serializer.getName() + "\" was not found (" + e.getStackTrace()[0] + ")");
                    } catch (InstantiationException e) {
                        System.err.println("Could not instantiate class \"" + serializer.getName() + "\" (" + e.getStackTrace()[0] + ")");
                    }
                }
                if (a instanceof JsonObjectField annotation) {
                    String name = annotation.name();
                    object.add(name, serialize(value));
                }
                if (a instanceof JsonPropertyField annotation) {
                    String name = annotation.name();
                    if (value instanceof Number val) {
                        object.addProperty(name, val);
                    } else if (value instanceof String val) {
                        object.addProperty(name, val);
                    } else if (value instanceof Boolean val) {
                        object.addProperty(name, val);
                    } else if (value instanceof Character val) {
                        object.addProperty(name, val);
                    } else { // Will attempt to convert it with GSON's built-in conversions like lists, and hashmaps
                        object.add(name, gson.fromJson(gson.toJson(value), JsonElement.class));
                    }
                }
            }
        }

        return object;
    }
}
