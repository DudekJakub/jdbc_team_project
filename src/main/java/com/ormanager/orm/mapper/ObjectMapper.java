package com.ormanager.orm.mapper;

import com.ormanager.App;
import com.ormanager.client.entity.Book;
import com.ormanager.client.entity.Publisher;
import com.ormanager.orm.OrmManager;
import com.ormanager.orm.annotation.Column;
<<<<<<< HEAD
import lombok.extern.slf4j.Slf4j;
=======
import com.ormanager.orm.annotation.ManyToOne;
import com.ormanager.orm.annotation.OneToMany;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
>>>>>>> 8ebaad126e0d70102e59a11e798078b6e5d8d8a5

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class ObjectMapper {

    public static <T> Optional<T> mapperToObject(ResultSet resultSet, T t) {
        try {
            for (Field field : t.getClass().getDeclaredFields()) {
                System.out.println(field.getName());
            }
            for (Field field : t.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                String fieldName = "";
                if (field.isAnnotationPresent(Column.class) && !field.getAnnotation(Column.class).name().equals("")) {
                    fieldName = field.getAnnotation(Column.class).name();
                } else {
                    fieldName = field.getName();
                }
                if (field.getType() == Integer.class) {
                    field.set(t, resultSet.getInt(fieldName));
                } else if (field.getType() == Long.class) {
                    field.set(t, resultSet.getLong(fieldName));
                } else if (field.getType() == String.class) {
                    field.set(t, resultSet.getString(fieldName));
                } else if (field.getType() == LocalDate.class) {
                    field.set(t, resultSet.getDate(fieldName).toLocalDate());
                }
            }
        } catch (IllegalAccessException | SQLException e) {
            LOGGER.info(String.valueOf(e));
        }
        return Optional.ofNullable(t);
    }

    public static <T> List<T> mapperToList(ResultSet resultSet, Class<?> cls) {
        List<T> list = new ArrayList<>();
        T t;
        try {
            t = (T) cls.getDeclaredConstructor().newInstance();
            while (resultSet.next()) {
                t = mapperToObject(resultSet, t).orElseThrow();
                list.add(t);
            }
        } catch (SQLException | NoSuchMethodException | InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            LOGGER.info(String.valueOf(e));
        }
        return list;
    }
}
