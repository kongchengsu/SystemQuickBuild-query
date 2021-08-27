package com.sukongcheng.query;
@FunctionalInterface
public interface QueryTypeConsumer<T,C,U,E,I> {
    void accept(T t,C c, U u,E e,I i);
}
