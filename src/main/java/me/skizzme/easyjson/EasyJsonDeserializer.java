package me.skizzme.easyjson;

import com.google.gson.*;
import me.skizzme.easyjson.annotation.JsonField;
import me.skizzme.easyjson.annotation.JsonInstantiateMethod;
import me.skizzme.easyjson.annotation.JsonObjectField;
import me.skizzme.easyjson.annotation.JsonPropertyField;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

public class EasyJsonDeserializer<T> {

    public static Gson gson = new GsonBuilder().create();

    /**
     * Sets all variables of the instance provided from the json.
     *
     * If there is a variable which is set to another deserializable object, it must either be instantiated or must have a constructor with no arguments.
     * @param json The JSON source object
     * @param instance Instance of the class that is being deserialized
     * @return The instance provided with all deserializable variables set
     */
    public T deserialize(JsonObject json, T instance) {
        Class c = instance.getClass();

        for (Field f : c.getDeclaredFields()) {
            f.setAccessible(true);
            Object value = null;
            try {
                value = f.get(instance);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            for (Annotation a : f.getDeclaredAnnotations()) {
                try {
                    if (a instanceof JsonField anno) {
                        f.set(instance, anno.serializer().getDeclaredMethod("deserialize", JsonObject.class).invoke(anno.serializer().getConstructors()[0].newInstance(), json.get(anno.name())));
                    }
                    if (a instanceof JsonObjectField anno) {
                        if (f.get(instance) == null) {
                            boolean method_found = false;
                            for (Method m : f.getType().getDeclaredMethods()) {
                                m.setAccessible(true);
                                for (Annotation annotation : m.getDeclaredAnnotations()) {
                                    if (annotation instanceof JsonInstantiateMethod) {
                                        f.set(instance, new EasyJsonDeserializer<>().deserialize(json.getAsJsonObject(anno.name()), m.invoke(null)));
                                        method_found = true;
                                        break;
                                    }
                                }
                            }
                            if (!method_found)
                                f.set(instance, new EasyJsonDeserializer<>().deserialize(json.getAsJsonObject(anno.name()), f.getType().getConstructors()[0].newInstance()));
                        } else {
                            f.set(instance, new EasyJsonDeserializer<>().deserialize(json.getAsJsonObject(anno.name()), f.get(instance)));
                        }
                    }
                    if (a instanceof JsonPropertyField anno) {
                        JsonPrimitive jvalue = json.get(anno.name()).getAsJsonPrimitive();
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
