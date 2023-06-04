package magictheinjecting;

import com.netease.mc.mod.network.common.Library;
import net.minecraft.launchwrapper.LaunchClassLoader;

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Set;

public class MagicTheInjecting extends Thread {
    public static byte[][] classes;

    private static Class getClass(ClassLoader cl, String name) throws ClassNotFoundException {
        return cl.loadClass(name);
    }

    private ClassLoader findClassLoader(){
        ClassLoader cl = null;
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            ClassLoader threadLoader;
            if (thread == null || thread.getContextClassLoader() == null || (threadLoader = thread.getContextClassLoader()).getClass() == null || threadLoader.getClass().getName() == null) continue;
            String loaderName = threadLoader.getClass().getName();
            if (!loaderName.contains("LaunchClassLoader") && !loaderName.contains("RelaunchClassLoader")) continue;
            cl = threadLoader;
            break;
        }

        if (cl == null) {
            throw new RuntimeException("ClassLoader is null");
        }

        this.setContextClassLoader(cl);
        return cl;
    }

    @Override
    public void run() {
        try {
            PrintWriter writer = new PrintWriter(System.getProperty("user.home") + File.separator + "eloader-log.txt", "UTF-8");
            writer.println("Starting!");
            writer.flush();

            try {
                final ClassLoader cl = this.findClassLoader();
                Class forgeEventHandlerAnnotation = getClass(cl, "net.minecraftforge.fml.common.Mod$EventHandler");
                Class modAnnotation = getClass(cl, "net.minecraftforge.fml.common.Mod");
                Class fmlInitializationEventClass = getClass(cl, "net.minecraftforge.fml.common.event.FMLInitializationEvent");
                Class fmlPreInitializationEventClass = getClass(cl, "net.minecraftforge.fml.common.event.FMLPreInitializationEvent");
                Method loadMethod = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, Integer.TYPE, Integer.TYPE, ProtectionDomain.class);
                loadMethod.setAccessible(true);
                writer.println("Loading " + classes.length + " classes");
                writer.flush();
                ArrayList<Object[]> mods = new ArrayList<>();
                writer.write("Bypass stage - 1");
                writer.flush();
                try {
                    final Class libClass = getClass(cl,"com.netease.mc.mod.network.common.Library");
                    final Method testMethod = libClass.getDeclaredMethod("test");
                    testMethod.setAccessible(true);
                    testMethod.invoke(null,new Object[0]);
                }catch (Exception e){
                    writer.println(e);
                    writer.flush();
                }

                writer.write("Bypass stage - 2");
                writer.flush();

                for (byte[] classData : classes) {
                    if (classData == null) {
                        throw new Exception("classData is null");
                    }
                    if (cl.getClass() == null) {
                        throw new Exception("getClass() is null");
                    }
                    try {
                        Class tClass = (Class)loadMethod.invoke(cl, null, classData, 0, classData.length, cl.getClass().getProtectionDomain());

                        try {
                            final Class cl2 = getClass(cl,"net.minecraft.launchwrapper.LaunchClassLoader");
                            if (cl2.isAssignableFrom(cl.getClass())){
                                final Field classLoaderExceptionsField = getClass(cl,"net.minecraft.launchwrapper.LaunchClassLoader").getDeclaredField("classLoaderExceptions");
                                classLoaderExceptionsField.setAccessible(true);
                                ((Set<String>) classLoaderExceptionsField.get(cl)).add(tClass.getName());
                            }
                        }catch (Exception e){
                            writer.println(e);
                            writer.flush();
                        }

                        if (tClass.getAnnotation(modAnnotation) == null) 
                        	continue;
                        Object[] mod = new Object[3];
                        mod[0] = tClass;
                        ArrayList<Method> fmlPreInitMethods = new ArrayList<Method>();
                        ArrayList<Method> fmlInitMethods = new ArrayList<Method>();
                        for (Method m : tClass.getDeclaredMethods()) {
                            if (m.getAnnotation(forgeEventHandlerAnnotation) != null && m.getParameterCount() == 1 && m.getParameterTypes()[0] == fmlInitializationEventClass) {
	                            m.setAccessible(true);
	                            fmlInitMethods.add(m);
                        	}
                            if (m.getAnnotation(forgeEventHandlerAnnotation) != null && m.getParameterCount() == 1 && m.getParameterTypes()[0] == fmlPreInitializationEventClass) {
	                            m.setAccessible(true);
	                            fmlPreInitMethods.add(m);
                        	}
                        }
                        mod[1] = fmlPreInitMethods;
                        mod[2] = fmlInitMethods;
                        mods.add(mod);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        throw new Exception("Exception on defineClass", e);
                    }
                }
                writer.write("Bypass stage - 3");
                writer.flush();
                try {
                    final Class libClass = getClass(cl,"com.netease.mc.mod.network.common.Library");
                    final Method testMethod = libClass.getDeclaredMethod("test");
                    testMethod.setAccessible(true);
                    testMethod.invoke(null,new Object[0]);
                }catch (Exception e){
                    writer.println(e);
                    writer.flush();
                }
                writer.println(classes.length + " loaded successfully");
                writer.flush();
                for (Object[] mod : mods) {
                	Class modClass = (Class) mod[0];
                	ArrayList<Method> fmlPreInitMethods = (ArrayList<Method>) mod[1];
                	ArrayList<Method> fmlInitMethods = (ArrayList<Method>) mod[2];
                	Object modInstance = null;

					try {
                        writer.println("Instancing " + modClass.getName());
                        writer.flush();
                        modInstance = modClass.newInstance();
                        writer.println("Instanced");
                        writer.flush();
                    }
                    catch (Exception e) {
                        writer.println("Genexeption on instancing: " + e);
                        e.printStackTrace(writer);
                        writer.flush();
                        throw new Exception("Exception on instancing", e);
                    }

                    for (Method preInitMethod : fmlPreInitMethods) {
	                    try {
	                        writer.println("Preiniting " + preInitMethod);
	                        writer.flush();
	                        writer.println("Preinited");
	                        writer.flush();
	                        preInitMethod.invoke(modInstance, new Object[]{null});
	                    }
	                    catch (InvocationTargetException e) {
	                        writer.println("InvocationTargetException on preiniting: " + e);
	                        e.getCause().printStackTrace(writer);
	                        writer.flush();
	                        throw new Exception("Exception on preiniting (InvocationTargetException)", e.getCause());
	                    }
	                    catch (Exception e) {
	                        writer.println("Genexeption on preiniting: " + e);
	                        e.printStackTrace(writer);
	                        writer.flush();
	                        throw new Exception("Exception on preiniting", e);
	                    }
                	}

                	for (Method initMethod : fmlInitMethods) {
	                    try {
	                        writer.println("Initing " + initMethod);
	                        writer.flush();
	                        writer.println("Inited");
	                        writer.flush();
	                        initMethod.invoke(modInstance, new Object[]{null});
	                    }
	                    catch (InvocationTargetException e) {
	                        writer.println("InvocationTargetException on initing: " + e);
	                        e.getCause().printStackTrace(writer);
	                        writer.flush();
	                        throw new Exception("Exception on initing (InvocationTargetException)", e.getCause());
	                    }
	                    catch (Exception e) {
	                        writer.println("Genexeption on initing: " + e);
	                        e.printStackTrace(writer);
	                        writer.flush();
	                        throw new Exception("Exception on initing", e);
	                    }
                	}
                }
                writer.println("Successfully injected");
                writer.flush();
            }
            catch (Throwable e) {
                e.printStackTrace(writer);
                writer.flush();
            }
            writer.close();
        }
        catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static int injectCP(byte[][] classes) {
        try {
            MagicTheInjecting.classes = classes;
            MagicTheInjecting t = new MagicTheInjecting();
            t.start();
        }
        catch (Exception t) {
            // empty catch block
        }
        return 0;
    }

    public static byte[][] getByteArray(int size) {
        return new byte[size][];
    }
}
