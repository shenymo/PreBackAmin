package com.xeasy.prebackanim.utils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import de.robv.android.xposed.XposedHelpers;

public class ReflectUtils2 {


    public static boolean isMethodOverridden(Class<?> childClass, String methodName, Class<?>... parameterTypes) {
        // 在继承链中找到声明该方法的父类
        Method parentMethod = findDeclaredMethodInHierarchy(childClass, methodName, parameterTypes);
        if (parentMethod == null) {
            return false; // 父类中不存在该方法
        }

        // 检查父类方法是否允许被重写
        int modifiers = parentMethod.getModifiers();
        if (Modifier.isPrivate(modifiers) || Modifier.isFinal(modifiers)) {
            return false;
        }

        // 检查子类是否声明了该方法
        try {
            Method childMethod = childClass.getDeclaredMethod(methodName, parameterTypes);
            // 确认方法由子类声明且返回类型兼容
            return childMethod.getDeclaringClass() == childClass
                    && parentMethod.getReturnType().isAssignableFrom(childMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            return false;
        }
    }


    /**
     * 判断方法是否由 targetClass 以下 childClass 以上 重写过
     * @param childClass 本类
     * @param targetClass 或许有的超类
     * @param methodName 方法名
     * @param parameterTypes 方法参数类型
     * @return 是否重写过
     */
    public static boolean isMethodExtended4ClassOverridden(Class<?> childClass, Class<?> targetClass, String methodName, Class<?>... parameterTypes) {
        // 在继承链中找到声明该方法的父类
        Method parentMethod = findDeclaredMethodInHierarchy(childClass, methodName, parameterTypes);
        if (parentMethod == null) {
            return false; // 父类中不存在该方法
        }

        // 检查父类方法是否允许被重写
        int modifiers = parentMethod.getModifiers();
        if (Modifier.isPrivate(modifiers) || Modifier.isFinal(modifiers)) {
            return false;
        }

        // 检查子类是否声明了该方法
        try {
            // 自己没有 并且 超类没有
            Method childMethod = childClass.getDeclaredMethod(methodName, parameterTypes);

            // 确认方法由子类声明且返回类型兼容
            return childMethod.getDeclaringClass() == childClass
                    && parentMethod.getReturnType().isAssignableFrom(childMethod.getReturnType());

        } catch (NoSuchMethodException e) {
            return ! parentMethod.getDeclaringClass().equals(targetClass);
        }
    }

    private static Method findDeclaredMethodInHierarchy(Class<?> childClass, String methodName, Class<?>... parameterTypes) {
        Class<?> currentClass = childClass.getSuperclass();
        while (currentClass != null && currentClass != Object.class) {
            try {
                return currentClass.getDeclaredMethod(methodName, parameterTypes);
            } catch (NoSuchMethodException e) {
                // 继续向上查找父类
                currentClass = currentClass.getSuperclass();
            }
        }
        return null;
    }

    // 示例使用
    /*public static void main(String[] args) {
        boolean overridden = isMethodOverridden(Child.class, "doSomething");
        System.out.println("方法是否被重写: " + overridden);
    }*/
}

/*class Parent {
    public void doSomething() {
    }
}

class Child extends Parent {
    @Override
    public void doSomething() {
    }
}*/


