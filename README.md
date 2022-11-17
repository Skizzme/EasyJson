# EasyJson
I made this because I found GSON's built-in serialization annotation system and didn't like it.

# Annotation Usage

## @JsonPropertyField
Used for all types supported by gson (int, long, String, boolean, etc)
```java
@JsonPropertyField(name = "example")
public String example = "";
```

## @JsonObjectField
Used for other types not automatically supported by gson. The built-in serializers will try to build a JsonObject from it.
```java
@JsonObjectField(name = "example")
public Example example = new Example();
```
Parameters: ``name=String``, [``fields=@SpecifyJsonField[]``](#specify_field), and ``methods=@SpecifyJsonGetterSetter[]``

##@JsonField
Used for when you want to create your own serialization

```java
@JsonField(name = "example", serializer = ExampleSerializer.class)
public Example example = new Example();
```
Serializer class:
```java
public class ExampleDeserializer extends JsonSerializer<Example> {
    @Override
    public JsonElement serialize(Example example) {
        JsonObject el = new JsonObject();
        el.addProperty("example_key", example.some_value);
        return el;
    }

    @Override
    public Example deserialize(JsonObject jsonObject) {
        return new Example(jsonObject.get("example_key").getAsString());
    }
}
```

## @JsonInstantiateMethod
Used for creating an instance of the class when using ``@JsonObjectField`` inside other objects being deserialized
```java
class Example {
    @JsonInstantiateMethod
    public static Example jsonInstantiate() {
        return new Example();
    }
}
```
Alternatively you can create a constructor with no parameters
```java
class Example {
    public Example() {}
}
```
Or you can manually instantiate the object
```java
class ExampleParent {
    @JsonObjectField(name = "example_object")
    public Example example = new Example();
}
```
## <a name="specify_field"></a>@SpecifyJsonField
Used to specify field names for an object

## Serializing
```java
EasyJson.serialize(example_object);
```
## Deserializing
```java
ExampleClass instance = new ExampleClass();
new EasyJsonDeserializer<ExampleClass>().deserialize(json_source_object, instance);
```
Or
```java
ExampleClass instance = new EasyJsonDeserializer<ExampleClass>().deserialize(json_source_object, new ExampleClass());
```