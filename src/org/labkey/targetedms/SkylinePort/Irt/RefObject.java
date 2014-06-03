package org.labkey.targetedms.SkylinePort.Irt;

/**
 * User: tgaluhn
 * Date: 5/27/2014
 *
 * Wrapper class used when converting C# parameters passed by ref.
 *
 */
public class RefObject<T> {
    private T referent;

    public RefObject(T initialValue) {
        referent = initialValue;
    }

    public void set(T newVal) {
        referent = newVal;
    }

    public T get() {
        return referent;
    }
}