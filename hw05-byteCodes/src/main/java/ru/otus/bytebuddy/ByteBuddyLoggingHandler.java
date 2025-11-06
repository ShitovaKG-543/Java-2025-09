package ru.otus.bytebuddy;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.otus.annotations.Log;

public class ByteBuddyLoggingHandler {
    private final Set<String> loggedMethods;
    private static final Logger logger = LoggerFactory.getLogger(ByteBuddyLoggingHandler.class);

    public ByteBuddyLoggingHandler(Class<?> targetClass) {
        this.loggedMethods = new HashSet<>();
        initializeLoggedMethods(targetClass);
    }

    private void initializeLoggedMethods(Class<?> targetClass) {
        // Проверяем методы в целевом классе и его родителях
        Class<?> currentClass = targetClass;
        while (currentClass != null && currentClass != Object.class) {
            for (Method method : currentClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Log.class)) {
                    String methodSignature = createMethodSignature(method);
                    loggedMethods.add(methodSignature);
                }
            }
            currentClass = currentClass.getSuperclass();
        }
    }

    @RuntimeType
    public Object intercept(
            @Origin Method method, @AllArguments Object[] args, @SuperCall java.util.concurrent.Callable<?> callable)
            throws Exception {

        String methodSignature = createMethodSignature(method);
        if (loggedMethods.contains(methodSignature)) {
            logMethodCall(method, args);
        }

        return callable.call();
    }

    private String createMethodSignature(Method method) {
        return method.getName() + Arrays.toString(method.getParameterTypes());
    }

    private void logMethodCall(Method method, Object[] args) {
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("executed method: ").append(method.getName());

        if (args != null && args.length > 0) {
            logMessage.append(", ");

            Parameter[] parameters = method.getParameters();
            for (int i = 0; i < args.length; i++) {
                if (i > 0) {
                    logMessage.append(", ");
                }
                String paramName = parameters[i].getName();
                logMessage.append(paramName).append(": ").append(args[i]);
            }
        }

        logger.info(logMessage.toString());
    }
}
