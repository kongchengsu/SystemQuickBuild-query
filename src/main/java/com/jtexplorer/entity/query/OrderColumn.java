package com.jtexplorer.entity.query;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
/**
 * sql排序参数注解
 * 用于在QueryParam（QueryParamOne）的子类中，注解出用于参与sql排序构建的属性，
 * 主要用于检验orderItem中的排序列是否符合要求
 *
 * @author 苏友朋
 * @date 2019/06/24 09:41
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface OrderColumn {
    /**
     * 条件查询列名，使用实体类中的属性名（会自动将驼峰转为下划线）即可（数据库列名也可以）
     * 默认like
     */
    String[] columnName() default "";
}
