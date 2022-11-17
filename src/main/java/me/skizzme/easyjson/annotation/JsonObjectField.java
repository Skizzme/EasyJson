package me.skizzme.easyjson.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JsonObjectField {
    String name();
    SpecifyJsonField[] fields() default {};
    SpecifyJsonGetterSetter[] methods() default {};
}
