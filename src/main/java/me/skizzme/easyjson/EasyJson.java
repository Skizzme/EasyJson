package me.skizzme.easyjson;

import com.google.gson.*;
import me.skizzme.easyjson.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

public class EasyJson {

    public static Gson gson = new GsonBuilder().create();

    public static JsonObject serialize(Object obj) {
        return serialize(obj, new SpecifyJsonField[0], new SpecifyJsonGetterSetter[0]);
    }

    public static JsonObject serialize(Object obj, SpecifyJsonField[] specified_fields, SpecifyJsonGetterSetter[] specified_methods) {
        JsonObject object = new JsonObject();
        HashMap<String, String> mapped_field_names = new HashMap<>();
        HashMap<String, String> mapped_method_getter_names = new HashMap<>();
        for (SpecifyJsonField fi : specified_fields) {
            if (!fi.variable_name().equals("")) mapped_field_names.put(fi.variable_name(), fi.json_name());
        }
        for (SpecifyJsonGetterSetter mi : specified_methods) {
            if (!mi.getter_method_name().equals("")) mapped_method_getter_names.put(mi.getter_method_name(), mi.json_name());
        }

        for (Method m : obj.getClass().getMethods()) {
            try {
                if (mapped_method_getter_names.containsKey(m.getName())) {
                    m.setAccessible(true);
                    addProperty(m.invoke(obj), object, mapped_method_getter_names.get(m.getName()), null);
                }
            } catch (InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        for (Field field : obj.getClass().getDeclaredFields()) {
            Annotation[] annotations = field.getDeclaredAnnotations();

            try {
                if (mapped_field_names.containsKey(field.getName())) {
                    field.setAccessible(true);
                    addProperty(field.get(obj), object, mapped_field_names.get(field.getName()), null);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

            for (Annotation a : annotations) {
                try {
                    if (a instanceof JsonField annotation) {
                        field.setAccessible(true);
                        Object value = field.get(obj);

                        String name = annotation.value();
                        Class<?> serializer = annotation.serializer();
                        try {
                            object.add(name, (JsonElement) serializer.getMethod("serialize", Object.class).invoke(serializer.getConstructors()[0].newInstance(), value));
                        } catch (IllegalAccessException e) {
                            System.err.println("Could not access \"serialize\" method for class \"" + obj.getClass().getName() + "\" (" + e.getStackTrace()[0] + ")");
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        } catch (NoSuchMethodException e) {
                            System.err.println("\"serialize\" method for class \"" + serializer.getName() + "\" was not found (" + e.getStackTrace()[0] + ")");
                        } catch (InstantiationException e) {
                            System.err.println("Could not instantiate class \"" + serializer.getName() + "\" (" + e.getStackTrace()[0] + ")");
                        }
                    }
                    if (a instanceof JsonObjectField annotation) {
                        field.setAccessible(true);
                        Object value = field.get(obj);

                        String name = annotation.value();
                        object.add(name, serialize(value, annotation.fields(), annotation.methods()));
                    }
                    if (a instanceof JsonPropertyField annotation) {
                        field.setAccessible(true);
                        addProperty(field.get(obj), object, null, annotation);
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InaccessibleObjectException e) {
                    e.printStackTrace();
                }
            }
        }

        return object;
    }

    private static void addProperty(Object value, JsonObject object, String name, JsonPropertyField annotation) throws IllegalAccessException {
        if (name == null && annotation != null)
            name = annotation.value();
        if (value instanceof Number val) {
            object.addProperty(name, val);
        } else if (value instanceof String val) {
            object.addProperty(name, val);
        } else if (value instanceof Boolean val) {
            object.addProperty(name, val);
        } else if (value instanceof Character val) {
            object.addProperty(name, val);
        } else if (value instanceof Iterable<?> val) {
            JsonArray array = new JsonArray();
            for (Object o : val) {
                array.add(serialize(o));
            }
            object.add(name, array);
        } else { // Will attempt to convert it with GSON's built-in conversions like lists, and hashmaps
            object.add(name, gson.fromJson(gson.toJson(value), JsonElement.class));
        }
    }
}
