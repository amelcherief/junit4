package org.junit.internal.runners.model;


/**
 * When invoked, throws the exception from the reflected method, rather than
 * wrapping it in an InvocationTargetException.
 */
public abstract class ReflectiveCallable {
    public Object run() throws Throwable {
        return runReflectiveCall();
    }

    protected abstract Object runReflectiveCall() throws Throwable;
}