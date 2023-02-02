package me.skizzme.easyjson;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public abstract class JsonSerializer<T> {
    public abstract JsonElement serialize(T obj);
    public abstract T deserialize(JsonObject json);
}