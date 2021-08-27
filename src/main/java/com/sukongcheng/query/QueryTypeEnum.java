package com.sukongcheng.query;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.sukongcheng.util.StringUtil;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

public enum QueryTypeEnum {
    EQ((f, c, o, q, u) -> {
        Object fieldValue = getFieldValue(o, f);
        if (StringUtil.isNotEmpty(fieldValue)) {
            q.eq(c, fieldValue);
            u.eq(c, fieldValue);
        }
    },"isEq"),
    LIKE((f, c, o, q, u) -> {
        Object fieldValue = getFieldValue(o, f);
        if (StringUtil.isNotEmpty(fieldValue)) {
            q.like(c, fieldValue);
            u.like(c, fieldValue);
        }
    },"isLike"),
    IN((f, c, o, q, u) -> {
        Object fieldValue = getFieldValue(o, f);
        if (StringUtil.isNotEmpty(fieldValue)) {
            q.in(c, fieldValue);
            u.in(c, fieldValue);
        }

    },"isIn"),
    NOT_EQ((f, c, o, q, u) -> {
        Object fieldValue = getFieldValue(o, f);
        if (StringUtil.isNotEmpty(fieldValue)) {
            q.ne(c, fieldValue);
            u.ne(c, fieldValue);
        }

    },"isNotEq"),
    NOT_IN((f, c, o, q, u) -> {
        Object fieldValue = getFieldValue(o, f);
        if (StringUtil.isNotEmpty(fieldValue)) {
            q.notIn(c, fieldValue);
            u.notIn(c, fieldValue);
        }
    },"isNotIn"),
    GE((f, c, o, q, u) -> {
        Object fieldValue = getFieldValue(o, f);
        if (StringUtil.isNotEmpty(fieldValue)) {
            q.ge(c, fieldValue);
            u.ge(c, fieldValue);
        }
    },"isGe"),
    LE((f, c, o, q, u) -> {
        Object fieldValue = getFieldValue(o, f);
        if (StringUtil.isNotEmpty(fieldValue)) {
            q.le(c, fieldValue);
            u.le(c, fieldValue);
        }
    },"isLe");

    private QueryTypeConsumer<Field, String, Object, QueryWrapper, UpdateWrapper> action;
    private String paramPrefix;
    public String getParamPrefix(){
        return paramPrefix;
    }

    QueryTypeEnum(QueryTypeConsumer<Field, String, Object, QueryWrapper, UpdateWrapper> action,String paramPrefix) {
        this.action = action;
        this.paramPrefix = paramPrefix;
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
    private static String camelToUnderline(String param, Integer charType) {
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
