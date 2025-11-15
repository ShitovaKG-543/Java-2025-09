package ru.otus.bytebuddy;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;

public class ProxyFactory {

    @SuppressWarnings("unchecked")
    public static <T> T createProxy(T target) {
        try {
            Class<? extends T> proxyClass = new ByteBuddy()
                    .subclass((Class<T>) target.getClass())
                    .method(ElementMatchers.isDeclaredBy(target.getClass())
                            .or(ElementMatchers.isDeclaredBy(Object.class)))
                    .intercept(MethodDelegation.to(new ByteBuddyLoggingHandler(target.getClass())))
                    .make()
                    .load(target.getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                    .getLoaded();

            return proxyClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Byte Buddy proxy", e);
        }
    }
}
