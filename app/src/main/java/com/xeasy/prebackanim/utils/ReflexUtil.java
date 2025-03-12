package com.xeasy.prebackanim.utils;



import static com.xeasy.prebackanim.hook.HookImpl.IS_LOG;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import de.robv.android.xposed.XposedBridge;

public class ReflexUtil {

    public static Method findMethodIfParamExistWithLog(Class<?> clazz, String methodName, Class<?> ... params ) {
        Class<?> clazzTemp = clazz;
        while (clazzTemp != null ) {
            loopOut:
            for (Method declaredMethod : clazzTemp.getDeclaredMethods()) {
                XposedBridge.log(" 类名 = " + clazzTemp.getName() +  " 方法名=== " + declaredMethod.getName());
                if ( methodName.equals(declaredMethod.getName()) ) {
                    declaredMethod.setAccessible(true);
                    // 如果要查的有参数 比较参数是否存在 不考虑前后顺序
                    if ( null != params && params.length > 0 ) {
                        Class<?>[] parameterTypes = declaredMethod.getParameterTypes();
                        if ( parameterTypes.length == 0 ) {
                            // 该方法没有参数, 进入下个循环
                            continue;
                        }
                        List<Class<?>> classes = Arrays.asList(parameterTypes);
                        for (Class<?> arg : params) {
                            if (! classes.contains(arg) ) {
                                continue loopOut;
                            }
                        }
                        return declaredMethod;
                    } else {
                        return declaredMethod;
                    }
                }
            }
            clazzTemp = clazzTemp.getSuperclass();
        }
        return null;
    }

    public static Method findMethodIfParamExist(Class<?> clazz, String methodName, Class<?> ... params ) {
        Class<?> clazzTemp = clazz;
        while (clazzTemp != null ) {
            loopOut:
            for (Method declaredMethod : clazzTemp.getDeclaredMethods()) {
//                XposedBridge.log(" 类名 = " + clazzTemp.getName() +  " 方法名=== " + declaredMethod.getName());
                if ( methodName.equals(declaredMethod.getName()) ) {
                    declaredMethod.setAccessible(true);
                    // 如果要查的有参数 比较参数是否存在 不考虑前后顺序
                    if ( null != params && params.length > 0 ) {
                        Class<?>[] parameterTypes = declaredMethod.getParameterTypes();
                        if ( parameterTypes.length == 0 ) {
                            // 该方法没有参数, 进入下个循环
                            continue;
                        }
                        List<Class<?>> classes = Arrays.asList(parameterTypes);
                        for (Class<?> arg : params) {
                            if (! classes.contains(arg) ) {
                                continue loopOut;
                            }
                        }
                        return declaredMethod;
                    } else {
                        return declaredMethod;
                    }
                }
            }
            clazzTemp = clazzTemp.getSuperclass();
        }
        return null;
    }

    public static Object findField4ObjAndLog(Class<?> clazz, Object object, String fieldName) {
        if ( IS_LOG ) XposedBridge.log( "##############  findField4ObjAndLog: clazz = " + clazz + ", fieldName = " + fieldName );

        try {
            Field[] declaredFields = clazz.getDeclaredFields();
            for (Field declaredField : declaredFields) {
                declaredField.setAccessible(true);
                if ( IS_LOG ) XposedBridge.log( "###### 字段类型 = " + declaredField.getType()  );
                if ( IS_LOG ) XposedBridge.log( "###### 字段名 = " + declaredField.getName()  );
                if ( IS_LOG ) XposedBridge.log( "###### 字段值 = " + declaredField.get(object) );
                if ( fieldName.equals(declaredField.getName())) {
                    return declaredField;
                }

            }

            if ( clazz != Object.class ) {
                return findField4ObjAndLog(clazz.getSuperclass(), object, fieldName);
            }
            if ( IS_LOG ) XposedBridge.log( "#####  findField4ObjAndLog: 未找到字段!!!");
            return null;
        } catch ( Exception e) {
            return null;
        }
    }

    public static Runnable findRemoveRunnable(Class<?> clazz, Object object) {
        try {
            Field[] declaredFields = clazz.getDeclaredFields();
            for (Field declaredField : declaredFields) {
                declaredField.setAccessible(true);
                Object fieldValue = declaredField.get(object);
                if ( declaredField.getName().toLowerCase().contains("remove") && fieldValue instanceof Runnable ) {
                    return (Runnable) fieldValue;
                }
            }
            if ( clazz != Object.class ) {
                return findRemoveRunnable(clazz.getSuperclass(), object);
            }
            if ( IS_LOG ) XposedBridge.log( "#####  findRemoveRunnable: 未找到字段!!!");
            return null;
        } catch ( Exception e) {
            return null;
        }
    }


    public static Object getField4Obj(Object object, String fieldName) {
        Object field4Obj = getField4Obj(object.getClass(), object,  fieldName);
        if ( field4Obj == null ) {
            field4Obj = getField4Obj(object.getClass().getSuperclass(), object, fieldName);
        }
        return field4Obj;
    }
    public static Object getField4Obj(Class<?> clazz, Object object, String fieldName) {
        try {
            Field declaredField = clazz.getDeclaredField(fieldName);
            declaredField.setAccessible(true);
            return declaredField.get(object);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Object getField4ObjByClass(Class<?> clazz, Object object, Class<?> fieldClass) {
        try {
            Field[] declaredFields = clazz.getDeclaredFields();
            for (Field declaredField : declaredFields) {
                declaredField.setAccessible(true);
                if ( declaredField.getType() == fieldClass ) {
                    return declaredField.get(object);
                }
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void setField4Obj(String fieldName, Object object, Object value) {
        try {
            Field declaredField = object.getClass().getDeclaredField(fieldName);
            declaredField.setAccessible(true);
            declaredField.set(object, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Method getMethod4Class(Class<?> clazz, String methodName, Class<?>... paramTypes) {

//        if ( IS_LOG ) XposedBridge.log( "##############  getMethod4Class: clazz = " + clazz + ", methodName = " + methodName + ", params = " + paramTypes );

        if ( clazz.toString().contains("HeadsUp")) {
            for (Method declaredMethod : clazz.getDeclaredMethods()) {
                declaredMethod.setAccessible(true);
//                if ( IS_LOG ) XposedBridge.log("函数xx." + declaredMethod.getName());
                for (Class<?> parameterType : declaredMethod.getParameterTypes()) {
//                    if ( IS_LOG ) XposedBridge.log("parameterType =" + parameterType);
                }
            }
        }


        try {
            return clazz.getDeclaredMethod(methodName, paramTypes);
        } catch (NoSuchMethodException e) {
            return getMethod4Class(Objects.requireNonNull(clazz.getSuperclass()), methodName, paramTypes);
        }
    }


    public static Object runMethod(Object object, String methodName, Object[] params, Class<?>... paramTypes) {
//        if ( IS_LOG ) XposedBridge.log( "runMethod: object = " + object + ", methodName = " + methodName + ", params = " + params );
        try {
            if (null == params || params.length == 0) {
//                Method declaredMethod = object.getClass().getDeclaredMethod(methodName);
                Method declaredMethod = getMethod4Class(object.getClass(), methodName);
                declaredMethod.setAccessible(true);
                return declaredMethod.invoke(object);
            }

            Method method = getMethod4Class(object.getClass(), methodName, paramTypes);
            method.setAccessible(true);
            return method.invoke(object, params);
        } catch (Exception e) {
            if(e instanceof InvocationTargetException){
                return  ((InvocationTargetException) e).getTargetException();
            }
            return e;
        }
    }

    public static Object runStaticMethod(Class<?> clazz, String methodName, Object[] params, Class<?>... paramTypes) throws Exception {


        try {
            if (null == params || params.length == 0) {
                Method declaredMethod = clazz.getDeclaredMethod(methodName);
                declaredMethod.setAccessible(true);
                return declaredMethod.invoke(null);
            }

            Method method = clazz.getDeclaredMethod(methodName, paramTypes);
            method.setAccessible(true);
            return method.invoke(null, params);
        } catch (Exception e) {
            return e;
        }
    }

}
