package me.skizzme.easyjson.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SpecifyJsonGetterSetter {
    String json_name();
    String getter_name() default "";
    String setter_name() default "";
}
