package me.skizzme.easyjson;

import com.google.gson.*;
import me.skizzme.easyjson.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.Collection;
import java.util.HashMap;

public class EasyJsonDeserializer<T> {

    public static Gson gson = new GsonBuilder().create();

    public T deserialize(JsonObject json, T instance) {
        return this.deserialize(json, instance, new SpecifyJsonField[0], new SpecifyJsonGetterSetter[0]);
    }

    /**
     * Sets all variables of the instance provided from the json.
     *
     * If there is a variable which is set to another deserializable object, it must either be instantiated or must have a constructor with no arguments.
     * @param json The JSON source object
     * @param instance Instance of the class that is being deserialized
     * @return The instance provided with all deserializable variables set
     */
    public T deserialize(JsonObject json, T instance, SpecifyJsonField[] specified_fields, SpecifyJsonGetterSetter[] specified_methods) {
        Class c = instance.getClass();
        HashMap<String, String> mapped_field_names = new HashMap<>();
        HashMap<String, String> mapped_method_setter_names = new HashMap<>();
        for (SpecifyJsonField fi : specified_fields) {
            if (!fi.variable_name().equals("")) mapped_field_names.put(fi.variable_name(), fi.json_name());
        }
        for (SpecifyJsonGetterSetter mi : specified_methods) {
            if (!mi.setter_method_name().equals("")) mapped_method_setter_names.put(mi.setter_method_name(), mi.json_name());
        }

        for (Method m : c.getMethods()) {
            try {
                System.out.println("MAPPED:" + mapped_method_setter_names);
                System.out.println("JSON:" + json);
                if (mapped_method_setter_names.containsKey(m.getName())) {
                    JsonPrimitive jvalue = json.get(mapped_method_setter_names.get(mapped_method_setter_names.get(m.getName()))).getAsJsonPrimitive();
                    if (jvalue.isString()) {
                        m.invoke(instance, jvalue.getAsString());
                    }
                    if (jvalue.isBoolean()) {
                        m.invoke(instance, jvalue.getAsBoolean());
                    }
                    if (jvalue.isNumber()) {
                        m.invoke(instance, jvalue.getAsNumber());
                    }
                }
            } catch (InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        for (Field f : c.getDeclaredFields()) {
            try {
                if (mapped_field_names.containsKey(f.getName())) {
                    f.setAccessible(true);
                    JsonPrimitive jvalue = json.get(mapped_field_names.get(f.getName())).getAsJsonPrimitive();
                    if (jvalue.isString()) {
                        f.set(instance, jvalue.getAsString());
                    }
                    if (jvalue.isBoolean()) {
                        f.set(instance, jvalue.getAsBoolean());
                    }
                    if (jvalue.isNumber()) {
                        f.set(instance, jvalue.getAsNumber());
                    }
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

            for (Annotation a : f.getDeclaredAnnotations()) {
                try {
                    if (a instanceof JsonField anno) {
                        f.setAccessible(true);
                        f.set(instance, anno.serializer().getDeclaredMethod("deserialize", JsonObject.class).invoke(anno.serializer().getConstructors()[0].newInstance(), json.get(anno.value())));
                    }
                    if (a instanceof JsonObjectField anno) {
                        f.setAccessible(true);
                        if (f.get(instance) == null) {
                            boolean method_found = false;
                            for (Method m : f.getType().getDeclaredMethods()) {
                                m.setAccessible(true);
                                for (Annotation annotation : m.getDeclaredAnnotations()) {
                                    if (annotation instanceof JsonInstantiateMethod) {
                                        f.set(instance, new EasyJsonDeserializer<>().deserialize(json.getAsJsonObject(anno.value()), m.invoke(null)));
                                        method_found = true;
                                        break;
                                    }
                                }
                            }
                            if (!method_found)
                                for (Constructor<?> con : f.getType().getConstructors()) {
                                    if (con.getParameterCount() == 0) f.set(instance, new EasyJsonDeserializer<>().deserialize(json.getAsJsonObject(anno.value()), f.getType().getConstructors()[0].newInstance()));
                                }
                        } else {
                            f.set(instance, new EasyJsonDeserializer<>().deserialize(json.getAsJsonObject(anno.value()), f.get(instance)));
                        }
                    }
                    if (a instanceof JsonPropertyField anno) {
                        f.setAccessible(true);
                        JsonElement field_json = json.get(anno.value());
                        if (field_json.isJsonPrimitive()) {
                            JsonPrimitive jvalue = field_json.getAsJsonPrimitive();
                            if (jvalue.isString()) {
                                f.set(instance, jvalue.getAsString());
                            }
                            if (jvalue.isBoolean()) {
                                f.set(instance, jvalue.getAsBoolean());
                            }
                            if (jvalue.isNumber()) {
                                f.set(instance, jvalue.getAsNumber());
                            }
                        } else if (field_json.isJsonArray()) {
                            Collection field_array = (Collection) f.get(instance);
//                            System.out.println(((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0].);
                            // HANDLE CONSTRUCTORS PROPERLY AND GIVE PROPER EXCEPTIONS
                            for (JsonElement element : field_json.getAsJsonArray()) {
                                field_array.add(new EasyJsonDeserializer<>().deserialize(element.getAsJsonObject(), ((Class) ((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0]).getConstructors()[0].newInstance()));
                            }
                        }
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
            }
        }
        return instance;
    }
}
