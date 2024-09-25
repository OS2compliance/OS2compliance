package dk.digitalidentity.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ReflectionHelper {

    public static <T> void callSetterWithParam(final Class<T> clazz, final T obj, final String fieldName, final String value) {
        final String setterName = "set" + StringUtils.capitalize(fieldName);
        final Field field = ReflectionUtils.findField(clazz, fieldName);
        if (field == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not find field");
        }
        final Method method = ReflectionUtils.findMethod(clazz, setterName, field.getType());
        if (method == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not find setter method");
        }
        final String typeName = field.getType().getName();
        if (typeName.equals("boolean")) {
            ReflectionUtils.invokeMethod(method, obj, Boolean.valueOf(value));
        } else if (typeName.equals("java.lang.Long")) {
            ReflectionUtils.invokeMethod(method, obj, Long.parseLong(value));
        } else if (typeName.equals(String.class.getName())) {
            ReflectionUtils.invokeMethod(method, obj, value);
        }
    }

}
