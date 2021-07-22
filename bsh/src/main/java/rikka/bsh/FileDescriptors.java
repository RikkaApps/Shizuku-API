package rikka.bsh;

import android.annotation.SuppressLint;

import java.io.FileDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@SuppressLint("DiscouragedPrivateApi")
@SuppressWarnings("JavaReflectionMemberAccess")
class FileDescriptors {

    private static Method getInt;
    private static Method setInt;

    static {
        try {
            getInt = FileDescriptor.class.getDeclaredMethod("getInt$");
            getInt.setAccessible(true);
            setInt = FileDescriptor.class.getDeclaredMethod("setInt$", int.class);
            setInt.setAccessible(true);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    public static FileDescriptor fromFd(int fd) {
        FileDescriptor fileDescriptor = new FileDescriptor();
        try {
            setInt.invoke(fileDescriptor, fd);
        } catch (IllegalAccessException | InvocationTargetException ignored) {
        }
        return fileDescriptor;
    }

    public static int getFd(FileDescriptor fileDescriptor) {
        try {
            //noinspection ConstantConditions
            return (int) getInt.invoke(fileDescriptor);
        } catch (IllegalAccessException | InvocationTargetException ignored) {
            return -1;
        }
    }
}
