package com.sun.jna;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/**
 * Type conversion between java types and native types.
 *
 * @author stefan@endrullis.de
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface TypeConversion {
    Class converter();
}
