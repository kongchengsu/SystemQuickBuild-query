package com.jtexplorer.entity.query;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.jtexplorer.util.StringUtil;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

public enum QueryTypeEnum {
    EQ((f, c, o, q, u) -> {
        Object fieldValue = getFieldValue(o, f);
        if (StringUtil.isNotEmpty(fieldValue)) {
            q.eq(c, fieldValue);
            u.eq(c, fieldValue);
        }
    }),
    LIKE((f, c, o, q, u) -> {
        Object fieldValue = getFieldValue(o, f);
        if (StringUtil.isNotEmpty(fieldValue)) {
            q.like(c, fieldValue);
            u.like(c, fieldValue);
        }
    }),
    IN((f, c, o, q, u) -> {
        Collection fieldValue = (Collection) getFieldValue(o, f);
        if (StringUtil.isNotEmpty(fieldValue)) {
            q.in(c, fieldValue);
            u.in(c, fieldValue);
        }

    }),
    NOT_EQ((f, c, o, q, u) -> {
        Object fieldValue = getFieldValue(o, f);
        if (StringUtil.isNotEmpty(fieldValue)) {
            q.ne(c, fieldValue);
            u.ne(c, fieldValue);
        }

    }),
    NOT_IN((f, c, o, q, u) -> {
        Collection fieldValue = (Collection) getFieldValue(o, f);
        if (StringUtil.isNotEmpty(fieldValue)) {
            q.notIn(c, fieldValue);
            u.notIn(c, fieldValue);
        }
    }),
    GE((f, c, o, q, u) -> {
        Object fieldValue = getFieldValue(o, f);
        if (StringUtil.isNotEmpty(fieldValue)) {
            q.ge(c, fieldValue);
            u.ge(c, fieldValue);
        }
    }),
    LE((f, c, o, q, u) -> {
        Object fieldValue = getFieldValue(o, f);
        if (StringUtil.isNotEmpty(fieldValue)) {
            q.le(c, fieldValue);
            u.le(c, fieldValue);
        }
    });

    private QueryTypeConsumer<Field, String, Object, QueryWrapper, UpdateWrapper> action;

    QueryTypeEnum(QueryTypeConsumer<Field, String, Object, QueryWrapper, UpdateWrapper> action) {
        this.action = action;
    }

    private QueryTypeConsumer<Field, String, Object, QueryWrapper, UpdateWrapper> getAction() {
        return action;
    }

    private static Object getFieldValue(Object o, Field f) {
        try {
            return o.getClass().getMethod("get" + upperFirstLatter(f.getName())).invoke(o);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void buildQuery(Field field, Object o,
                           QueryWrapper query, UpdateWrapper update) {
        this.action.accept(field, camelToUnderline(field.getName(), 1), o, query, update);
    }

    public void buildQuery(Field field, Object o, String columnName,
                           QueryWrapper query, UpdateWrapper update) {
        this.action.accept(field, camelToUnderline(columnName,1), o, query, update);
    }

    private static final char UNDERLINE = '_';

    /*驼峰转下划线*/
    public static String camelToUnderline(String param, Integer charType) {
        if (param == null || "".equals(param.trim())) {
            return "";
        }
        int len = param.length();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = param.charAt(i);
            if (Character.isUpperCase(c) && i>0) {
                sb.append(UNDERLINE);
            }
            if (charType == 2) {
                sb.append(Character.toUpperCase(c));  //统一都转大写
            } else {
                sb.append(Character.toLowerCase(c));  //统一都转小写
            }
        }
        return sb.toString();
    }

    public static String upperFirstLatter(String letter) {
        return letter.substring(0, 1).toUpperCase() + letter.substring(1);
    }
}
