package me.skizzme.easyjson.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import me.skizzme.easyjson.annotation.*;
import me.skizzme.easyjson.exception.NoInstantiationMethod;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

public class Deserializer<T> {

    public T deserialize(JsonObject json, T instance) {
        return this.deserialize(json, instance, new SpecifyJsonField[0], new SpecifyJsonGetterSetter[0]);
    }

    public Collection<T> deserializeList(JsonArray json, T instance) {
        Collection<T> list = new ArrayList<>();
        for (JsonElement e : json) {
            try {
                list.add(this.deserialize(e.getAsJsonObject(), instance, new SpecifyJsonField[0], new SpecifyJsonGetterSetter[0]));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return list;
    }

    /**
     * Sets all variables of the instance provided from the json.
     *
     * If there is a variable which is set to another deserializable object, it must either be instantiated or must have a constructor with no arguments.
     * @param json The JSON source object
     * @param instance Instance of the class that is being deserialized
     * @return The instance provided with all deserializable variables set
     */
    // TODO make it so that the deserializer doesn't necessarily have to have an instance, and could instead create an instance by finding an appropriate initializer and calling it with found vars
    public T deserialize(JsonObject json, T instance, SpecifyJsonField[] specified_fields, SpecifyJsonGetterSetter[] specified_methods) {
        if (instance == null) {
            throw new NullPointerException("Instance is null for json: " + json);
        }
        Class<?> c = instance.getClass();

        //Mapping fields and setters as variable_name -> json_name
        HashMap<String, String> mapped_field_names = new HashMap<>();
        HashMap<String, String> mapped_method_setter_names = new HashMap<>();
        for (SpecifyJsonField fi : specified_fields) {
            if (!fi.variable_name().equals("")) mapped_field_names.put(fi.variable_name(), fi.json_name());
        }

        for (SpecifyJsonGetterSetter mi : specified_methods) {
            if (!mi.setter_name().equals("")) mapped_method_setter_names.put(mi.setter_name(), mi.json_name());
        }

        //Checks methods for if they are specified setter
        for (Method m : c.getMethods()) {
            try {
                if (mapped_method_setter_names.containsKey(m.getName())) {
                    JsonPrimitive jvalue = json.get(mapped_method_setter_names.get(m.getName())).getAsJsonPrimitive();
                    if (jvalue.isString()) {
                        m.invoke(instance, jvalue.getAsString());
                    }
                    if (jvalue.isBoolean()) {
                        m.invoke(instance, jvalue.getAsBoolean());
                    }
                    if (jvalue.isNumber()) {
                        Class cl = m.getParameterTypes()[0];
                        if (cl == double.class) {
                            m.invoke(instance, jvalue.getAsDouble());
                        } else {
                            m.invoke(instance, jvalue.getAsNumber());
                        }
                    }
                }
            } catch (InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        // Checks fields and then sets them using the provided JSON object
        for (Field f : c.getDeclaredFields()) {
            f.setAccessible(true);
            try {
                if (mapped_field_names.containsKey(f.getName())) {
                    f.setAccessible(true);
                    setJsonPrimitive(json.get(mapped_field_names.get(f.getName())).getAsJsonPrimitive(), f, instance);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            for (Annotation a : f.getDeclaredAnnotations()) {
                try {
                    if (a instanceof JsonField) {
                        JsonField anno = (JsonField) a;
                        f.set(instance, anno.serializer().getDeclaredMethod("deserialize", JsonObject.class).invoke(anno.serializer().getConstructors()[0].newInstance(), json.get(anno.name())));
                    }
                    else if (a instanceof JsonObjectField) {
                        JsonObjectField anno = (JsonObjectField) a;

                        JsonElement field_json = json.get(anno.name());
                        if (f.getType().isArray()) {
                            Object[] array = (Object[]) f.get(instance);
                            JsonArray json_array = field_json.getAsJsonArray();
                            for (int i = 0; i < json_array.size(); i++) {
                                if (!json_array.get(i).isJsonNull()) {
                                    array[i] = new Deserializer<>().deserialize(json_array.get(i).getAsJsonObject(), newInstance(f.getType().getComponentType()), anno.fields(), anno.methods());
                                }
                            }
                        } else if (f.get(instance) instanceof Collection) {
                            Collection field_array = (Collection) f.get(instance);
                            Object[] arr = field_array.stream().toArray();
                            int i = 0;
                            for (JsonElement element : field_json.getAsJsonArray()) {
                                Object o = newInstance(((Class) ((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0]));
                                if (field_array.size() <= i) {
                                    o = arr[i];
                                }
                                field_array.add(new Deserializer<>().deserialize(element.getAsJsonObject(), o, anno.fields(), anno.methods()));
                                i++;
                            }
                        }
                        // Creates an instance of the field type if it is not already set
                        Object f_instance;
                        if (f.get(instance) == null) {
                            f_instance = newInstance(f.getType());
                        } else {
                            f_instance = f.get(instance);
                        }
                        // Deserializes the json object into the field instance
                        if (!json.has(anno.name())) {
                            System.out.println("Json key '" + anno.name() + "' was not found in json source");
                            continue;
                        }
                        JsonElement object = json.get(anno.name());
                        f.set(instance,
                                object.isJsonNull() ?
                                        null :
                                        new Deserializer<>().deserialize(
                                                object.getAsJsonObject(),
                                                f_instance,
                                                anno.fields(),
                                                anno.methods()
                                        )
                        );
                    }
                    else if (a instanceof JsonPropertyField) {
                        JsonPropertyField anno = (JsonPropertyField)a;
                        JsonElement field_json = json.get(anno.name());

                        if (field_json != null) {
                            // Sets the enum from the ordinal integer
                            if (f.getType().isEnum()) {
                                f.set(instance, ((Object[]) f.getType().getMethod("values").invoke(f))[field_json.getAsJsonPrimitive().getAsInt()]);
                            } else if (f.getType().getSuperclass() == AbstractMap.class) {
                                AbstractMap casted = (AbstractMap) f.get(instance);
                                for (Map.Entry<String, JsonElement> stringJsonElementEntry : field_json.getAsJsonObject().entrySet()) {
                                    Map.Entry<String, JsonElement> s = (Map.Entry) stringJsonElementEntry;
                                    Object key = convertKeyTo(((Class) ((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0]), s.getKey());
                                    Object value = new Deserializer<>().deserialize(stringJsonElementEntry.getValue().getAsJsonObject(), newInstance(((Class) ((ParameterizedType) f.getGenericType()).getActualTypeArguments()[1])));
                                    casted.put(key, value);
                                }
                            } else if (field_json.isJsonPrimitive()) {
                                setJsonPrimitive(field_json.getAsJsonPrimitive(), f, instance);
                            }
                            // Will loop through each element of the json array and deserialize each one as the specified generic type of the field
                            else if (field_json.isJsonArray()) {
                                if (f.getType().isArray()) {
                                    Object[] array = (Object[]) f.get(instance);
                                    JsonArray json_array = field_json.getAsJsonArray();
                                    for (int i = 0; i < json_array.size(); i++) {
                                        if (!json_array.get(i).isJsonNull()) {
                                            array[i] = new Deserializer<>().deserialize(json_array.get(i).getAsJsonObject(), newInstance(f.getType().getComponentType()));
                                        }
                                    }
                                } else {
                                    Collection field_array = (Collection) f.get(instance);
                                    Object[] arr = field_array.stream().toArray();
                                    int i = 0;
                                    for (JsonElement element : field_json.getAsJsonArray()) {
                                        Object o = newInstance(((Class) ((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0]));
                                        if (field_array.size() < i) {
                                            o = arr[i];
                                        }
                                        field_array.add(getAsPrimitiveOrObject(element, ((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0], o));
                                        i++;
                                    }
                                }
                            }
                        }
                    }
                } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException | InstantiationException | NoInstantiationMethod e) {
                    e.printStackTrace();
                }
            }
            f.setAccessible(false);
        }
        return instance;
    }

    private static Object getAsPrimitiveOrObject(JsonElement o, Type type, Object instance) throws NoInstantiationMethod, InvocationTargetException, IllegalAccessException, InstantiationException {
        if (o.isJsonPrimitive()){
            JsonPrimitive p = o.getAsJsonPrimitive();
            if (p.isNumber()) {
                return p.getAsNumber();
            }
            if (p.isString()) {
                return p.getAsString();
            }
            if (p.isBoolean()) {
                return p.getAsBoolean();
            }
        }
        if (instance == null) instance = newInstance((Class) type);
        return new Deserializer<>().deserialize(o.getAsJsonObject(), newInstance((Class) type));
    }

    /**
     * Attempts to create a new instance of the given class using a constructor with no parameters or a method with the {@code @JsonInstantiationMethod} annotation.
     */
    public static Object newInstance(Class<?> clazz) throws InvocationTargetException, IllegalAccessException, InstantiationException, NoInstantiationMethod {
        for (Method m : clazz.getDeclaredMethods()) {
            try {
                m.setAccessible(true);
                for (Annotation a : m.getDeclaredAnnotations()) {
                    if (a instanceof JsonInstantiateMethod) return m.invoke(null);
                }
                m.setAccessible(false);
            } catch (Exception e) {
//                e.printStackTrace();
            }
        }
        for (Constructor<?> con : clazz.getConstructors()) {
            if (con.getParameterCount() == 0)
                return con.newInstance();
        }
        throw new NoInstantiationMethod("No instantiation method or viable constructor was found for " + clazz + ". Please manually set the field.");
    }

    private static Object convertKeyTo(Class<?> type, String value) throws NoInstantiationMethod, InvocationTargetException, IllegalAccessException, InstantiationException {
        if (type == int.class || type == Integer.class) {
            return Integer.parseInt(value);
        } else if (type == long.class || type == Long.class) {
            return Long.parseLong(value);
        } else if (type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(value);
        } else if (type == double.class || type == Double.class) {
            return Double.parseDouble(value);
        } else if (type == float.class || type == Float.class) {
            return Float.parseFloat(value);
        } else if (type == char.class || type == Character.class) {
            return (char) value.getBytes()[0];
        } else if (type == String.class) {
            return value;
        }
        return null;
    }

    private static Object getPrimitiveOrObject(Class<?> type, JsonObject value) throws NoInstantiationMethod, InvocationTargetException, IllegalAccessException, InstantiationException {
        if (type == int.class || type == Integer.class) {
            return value.getAsInt();
        } else if (type == long.class || type == Long.class) {
            return value.getAsLong();
        } else if (type == boolean.class || type == Boolean.class) {
            return value.getAsBoolean();
        } else if (type == double.class || type == Double.class) {
            return value.getAsDouble();
        } else if (type == float.class || type == Float.class) {
            return value.getAsFloat();
        } else if (type == char.class || type == Character.class) {
            return value.getAsCharacter();
        } else if (type == String.class) {
            return value.getAsString();
        }
        return newInstance(type);
    }

    private static void setJsonPrimitive(JsonPrimitive value, Field f, Object instance) throws IllegalAccessException {
        if (value.isString()) {
            f.set(instance, value.getAsString());
        }
        if (value.isBoolean()) {
            f.set(instance, value.getAsBoolean());
        }
        if (value.isNumber()) {
            if (f.getType() == double.class) {
                f.set(instance, value.getAsNumber().doubleValue());
            } else if (f.getType() == int.class) {
                f.set(instance, value.getAsNumber().intValue());
            } else if (f.getType() == long.class) {
                f.set(instance, value.getAsNumber().longValue());
            } else if (f.getType() == byte.class) {
                f.set(instance, value.getAsNumber().byteValue());
            } else if (f.getType() == float.class) {
                f.set(instance, value.getAsNumber().floatValue());
            } else if (f.getType() == short.class) {
                f.set(instance, value.getAsNumber().shortValue());
            } else {
                f.set(instance, (value.getAsNumber()));
            }
        }
    }
}
