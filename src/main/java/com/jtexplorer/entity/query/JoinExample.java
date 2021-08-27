package com.jtexplorer.entity.query;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JoinExample {
    QueryTypeEnum queryType() default QueryTypeEnum.LIKE;
    String columnName() default "";
}
