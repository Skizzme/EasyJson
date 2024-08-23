package me.skizzme.easyjson.impl;

import com.google.gson.*;
import me.skizzme.easyjson.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Serializer {

    public static JsonObject serialize(Object obj) {
        return serialize(obj, new SpecifyJsonField[0], new SpecifyJsonGetterSetter[0]);
    }

    public static JsonArray serialize(Collection<?> obj) {
        JsonArray array = new JsonArray();
        for (Object element : obj) {
            array.add(serialize(element));
        }
        return array;
    }

    public static JsonObject serialize(Map obj) {
        JsonObject object = new JsonObject();
        for (Object key : obj.keySet()) {
            object.add(serialize(key).toString(), serialize(obj.get(key)));
        }
        return object;
    }

    public static JsonObject serialize(Object obj, SpecifyJsonField[] specified_fields, SpecifyJsonGetterSetter[] specified_methods) {
        if (obj == null) {
            return null;
        }

        JsonObject object = new JsonObject();
        HashMap<String, String> mapped_field_names = new HashMap<>();
        HashMap<String, String> mapped_method_getter_names = new HashMap<>();
        for (SpecifyJsonField fi : specified_fields) {
            if (!fi.variable_name().equals("")) mapped_field_names.put(fi.variable_name(), fi.json_name());
        }
        for (SpecifyJsonGetterSetter mi : specified_methods) {
            if (!mi.getter_name().equals("")) mapped_method_getter_names.put(mi.getter_name(), mi.json_name());
        }

        // Will check the methods if they are specified getter methods, and if they are they wil
        for (Method m : obj.getClass().getMethods()) {
            try {
                if (mapped_method_getter_names.containsKey(m.getName())) {
                    m.setAccessible(true);
                    addProperty(m.invoke(obj), object, mapped_method_getter_names.get(m.getName()), null);
                }
            } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
                e.printStackTrace();
            }
        }

        for (Field field : obj.getClass().getDeclaredFields()) {
            Annotation[] annotations = field.getDeclaredAnnotations();

            field.setAccessible(true);
            try {
                // Checks the field if it is specified as a value field, if it is it will add the property
                if (mapped_field_names.containsKey(field.getName())) {
                    addProperty(field.get(obj), object, mapped_field_names.get(field.getName()), null);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }

            for (Annotation a : annotations) {
                try {
                    if (a instanceof JsonField) {
                        JsonField annotation = (JsonField) a;
                        field.setAccessible(true);
                        Object value = field.get(obj);

                        String name = annotation.name();
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
                    if (a instanceof JsonObjectField) {
                        JsonObjectField annotation = (JsonObjectField) a;
                        field.setAccessible(true);
                        Object value = field.get(obj);

                        String name = annotation.name();
                        object.add(name, serialize(value, annotation.fields(), annotation.methods()));
                    }
                    if (a instanceof JsonPropertyField) {
                        JsonPropertyField annotation = (JsonPropertyField) a;
                        field.setAccessible(true);
                        addProperty(field.get(obj), object, null, annotation);
                    }
                } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException |
                         NullPointerException e) {
                    e.printStackTrace();
                }
            }

            field.setAccessible(false);
        }

        return object;
    }

    private static JsonElement getAsPrimitiveOrObject(Object o) {
        if (o instanceof String) {
            return new JsonPrimitive((String) o);
        } else if (o instanceof Boolean) {
            return new JsonPrimitive((Boolean) o);
        } else if (o instanceof Number) {
            return new JsonPrimitive((Number) o);
        } else if (o instanceof Character) {
            return new JsonPrimitive((Character) o);
        } else {
            return serialize(o);
        }
    }

    private static void addProperty(Object value, JsonObject object, String name, JsonPropertyField annotation) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        if (name == null && annotation != null)
            name = annotation.name();

        if (value == null) {
            throw new NullPointerException("Object value is null with name \"" + name + "\" for JSON: " + object);
        }

        if (value.getClass().isEnum()) {
            object.addProperty(name, (int) value.getClass().getMethod("ordinal").invoke(value));
            return;
        }

        if (value instanceof Number) {
            object.addProperty(name, (Number) value);
        } else if (value instanceof String) {
            object.addProperty(name, (String) value);
        } else if (value instanceof Boolean) {
            object.addProperty(name, (Boolean) value);
        } else if (value instanceof Character) {
            object.addProperty(name, (Character) value);
        } else if (value instanceof Iterable<?>) {
            System.out.println(name);
            Iterable<?> val = (Iterable<?>) value;
            JsonArray array = new JsonArray();
            for (Object o : val) {
                array.add(getAsPrimitiveOrObject(o));
            }
            object.add(name, array);
        } else if (value instanceof Map) {
            Map map = (Map) value;
            JsonObject obj = new JsonObject();
            for (Object o : map.keySet()) {
//                addProperty(map.get(o), obj, o.toString(), null);
                obj.add(o.toString(), getAsPrimitiveOrObject(map.get(o)));
            }
            object.add(name, obj);
        } else if (value.getClass().isArray()) {
            JsonArray array = new JsonArray();
            for (Object o : (Object[]) value) {
                array.add(getAsPrimitiveOrObject(o));
            }
            object.add(name, array);
        } else {
            // Would attempt to convert it with GSON's built-in conversions like lists, and hashmaps
//            object.add(name, gson.fromJson(gson.toJson(value), JsonElement.class));
            object.add(name, serialize(value));
        }
    }
}
