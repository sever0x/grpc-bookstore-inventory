package com.sever0x.bookstore.validation;

import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

@Component
public class BookValidator {

    public boolean isValidRequest(Object request) {
        Field[] fields = request.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object value = field.get(request);
                if (value == null || (value instanceof String && ((String) value).isBlank()) ||
                        (value instanceof String && ((String) value).length() < 3) ||
                        (value instanceof Integer && ((Integer) value) < 0)) {
                    return false;
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }
}
