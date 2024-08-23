package me.skizzme.easyjson;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.skizzme.easyjson.impl.Serializer;

public class JsonBuilder {

    private JsonObject json;

    public static JsonBuilder create() {
        return new JsonBuilder(new JsonObject());
    }

    public static JsonBuilder create(JsonObject o) {
        return new JsonBuilder(o);
    }

    private JsonBuilder(JsonObject o) {
        this.json = o;
    }

    public JsonBuilder add(String key, Object o) {
        if (o instanceof JsonElement) {
            json.add(key, (JsonElement) o);
        } else {
            json.add(key, Serializer.serialize(o));
        }
        return this;
    }

    public JsonBuilder addProperty(String key, Number v) {
        json.addProperty(key, v);
        return this;
    }

    public JsonBuilder addProperty(String key, String v) {
        json.addProperty(key, v);
        return this;
    }

    public JsonBuilder addProperty(String key, Boolean v) {
        json.addProperty(key, v);
        return this;
    }

    public JsonObject build() {
        return this.json;
    }

}
