package me.skizzme.easyjson;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.skizzme.easyjson.annotation.SpecifyJsonField;
import me.skizzme.easyjson.annotation.SpecifyJsonGetterSetter;
import me.skizzme.easyjson.impl.Deserializer;
import me.skizzme.easyjson.impl.Serializer;

import java.util.Collection;
import java.util.Map;

public class EasyJson {

    public static JsonObject serialize(Object obj) {
        return Serializer.serialize(obj);
    }

    public static JsonArray serialize(Collection<?> obj) {
        return Serializer.serialize(obj);
    }

    public static JsonObject serialize(Map obj) {
        return Serializer.serialize(obj);
    }

    public static <T> T deserialize(JsonObject json, T instance) {
        return new Deserializer<T>().deserialize(json, instance, new SpecifyJsonField[0], new SpecifyJsonGetterSetter[0]);
    }

    public <T> Collection<T> deserializeList(JsonArray json, T instance) {
        return new Deserializer<T>().deserializeList(json, instance);
    }
}
