package com.haloclient.client.agent;

import org.objectweb.asm.*;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class HaloAgent {

    private static final java.util.Set<ClassLoader> addedLoaders = java.util.Collections.synchronizedSet(new java.util.HashSet<>());
    private static boolean initialized = false;
    private static ClassFileTransformer activeTransformer = null;

    public static void premain(String agentArgs, Instrumentation inst) {
        setup(agentArgs, inst);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        setup(agentArgs, inst);
    }

    private static java.io.File getAgentJarFile() {
        try {
            java.net.URL url = HaloAgent.class.getProtectionDomain().getCodeSource().getLocation();
            return new java.io.File(url.toURI());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void ensureJarInLoader(ClassLoader loader) {
        if (loader == null || loader == ClassLoader.getSystemClassLoader()) return;
        if (!addedLoaders.add(loader)) return;

        try {
            java.io.File jarFile = getAgentJarFile();
            if (jarFile == null || !jarFile.exists()) {
                System.err.println("[HaloAgent] Could not find agent JAR file!");
                return;
            }

            System.out.println("[HaloAgent] Attempting to add JAR to classloader: " + loader.getClass().getName());

            // 1. Try directly on loader
            if (injectJarIntoTarget(loader, jarFile)) {
                System.out.println("[HaloAgent] Successfully added JAR to classloader directly.");
                return;
            }

            // 2. Try on delegate field (for net.fabricmc.loader.impl.launch.knot.KnotClassLoader)
            try {
                java.lang.reflect.Field delegateField = loader.getClass().getDeclaredField("delegate");
                delegateField.setAccessible(true);
                Object delegate = delegateField.get(loader);
                if (delegate != null) {
                    if (injectJarIntoTarget(delegate, jarFile)) {
                        System.out.println("[HaloAgent] Successfully added JAR to classloader delegate.");
                        return;
                    }
                }
            } catch (Exception e) {
                // Ignore
            }

            System.err.println("[HaloAgent] Warning: Could not explicitly add agent JAR to classloader " + loader.getClass().getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean injectJarIntoTarget(Object target, java.io.File jarFile) {
        java.net.URL url = null;
        java.nio.file.Path path = null;
        try {
            url = jarFile.toURI().toURL();
            path = jarFile.toPath();
        } catch (Exception e) {
            // Ignore
        }

        // Try addCodeSource(Path)
        if (path != null) {
            try {
                java.lang.reflect.Method m = findMethod(target.getClass(), "addCodeSource", java.nio.file.Path.class);
                if (m != null) {
                    m.setAccessible(true);
                    m.invoke(target, path);
                    return true;
                }
            } catch (Exception e) {
                // Ignore
            }
        }

        // Try addCodeSource(URL)
        if (url != null) {
            try {
                java.lang.reflect.Method m = findMethod(target.getClass(), "addCodeSource", java.net.URL.class);
                if (m != null) {
                    m.setAccessible(true);
                    m.invoke(target, url);
                    return true;
                }
            } catch (Exception e) {
                // Ignore
            }
        }

        // Try addURL(URL)
        if (url != null) {
            try {
                java.lang.reflect.Method m = findMethod(target.getClass(), "addURL", java.net.URL.class);
                if (m != null) {
                    m.setAccessible(true);
                    m.invoke(target, url);
                    return true;
                }
            } catch (Exception e) {
                // Ignore
            }
        }

        // Try addUrl(URL)
        if (url != null) {
            try {
                java.lang.reflect.Method m = findMethod(target.getClass(), "addUrl", java.net.URL.class);
                if (m != null) {
                    m.setAccessible(true);
                    m.invoke(target, url);
                    return true;
                }
            } catch (Exception e) {
                // Ignore
            }
        }

        return false;
    }

    private static java.lang.reflect.Method findMethod(Class<?> clazz, String name, Class<?> paramType) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredMethod(name, paramType);
            } catch (NoSuchMethodException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static void setup(String agentArgs, Instrumentation inst) {
        if ("uninject".equals(agentArgs)) {
            uninject(inst);
            return;
        }

        System.out.println("[HaloAgent] Initializing Java Agent...");
        System.setProperty("halo.injected", "true");

        // Dynamically register mixin config at runtime if not already done
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            if (loader == null || loader == ClassLoader.getSystemClassLoader()) {
                for (Class<?> clazz : inst.getAllLoadedClasses()) {
                    if (clazz.getName().equals("net.minecraft.client.Minecraft")) {
                        loader = clazz.getClassLoader();
                        break;
                    }
                }
            }
            if (loader != null && loader != ClassLoader.getSystemClassLoader()) {
                try {
                    Class<?> mixinsClass = loader.loadClass("org.spongepowered.asm.mixin.Mixins");
                    java.lang.reflect.Method addConfig = mixinsClass.getMethod("addConfiguration", String.class);
                    addConfig.invoke(null, "halo.client.mixins.json");
                    System.out.println("[HaloAgent] Dynamically registered mixin config 'halo.client.mixins.json'");
                } catch (Exception mixinEx) {
                    System.out.println("[HaloAgent] Dynamic mixin registration skipped: " + mixinEx.getMessage());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (activeTransformer != null) {
            System.out.println("[HaloAgent] Transformer already registered.");
            return;
        }

        activeTransformer = new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                    ProtectionDomain protectionDomain, byte[] classfileBuffer) {
                if (!"true".equals(System.getProperty("halo.injected"))) {
                    return null;
                }
                if (className == null) return null;

                // Inject our agent JAR into the target classloader
                ensureJarInLoader(loader);

                try {
                    if (className.equals("net/minecraft/client/renderer/GameRenderer")) {
                        System.out.println("[HaloAgent] Transforming GameRenderer...");
                        return transformGameRenderer(classfileBuffer);
                    }
                    if (className.equals("net/minecraft/client/gui/Gui")) {
                        System.out.println("[HaloAgent] Transforming Gui...");
                        return transformGui(classfileBuffer);
                    }
                    if (className.equals("net/minecraft/client/KeyboardHandler")) {
                        System.out.println("[HaloAgent] Transforming KeyboardHandler...");
                        return transformKeyboard(classfileBuffer);
                    }
                } catch (Exception e) {
                    System.err.println("[HaloAgent] Error transforming class " + className);
                    e.printStackTrace();
                }
                return null;
            }
        };

        inst.addTransformer(activeTransformer, true);

        // Retransform already loaded classes (for agent attachment)
        for (Class<?> clazz : inst.getAllLoadedClasses()) {
            String name = clazz.getName();
            if (name.equals("net.minecraft.client.renderer.GameRenderer") ||
                name.equals("net.minecraft.client.gui.Gui") ||
                name.equals("net.minecraft.client.KeyboardHandler")) {
                try {
                    System.out.println("[HaloAgent] Retransforming loaded class: " + name);
                    inst.retransformClasses(clazz);
                } catch (Exception e) {
                    System.err.println("[HaloAgent] Failed to retransform class: " + name);
                    e.printStackTrace();
                }
            }
        }
    }

    private static void uninject(Instrumentation inst) {
        System.out.println("[HaloAgent] Uninjecting agent...");
        System.getProperties().remove("halo.injected");

        if (activeTransformer != null) {
            inst.removeTransformer(activeTransformer);
            activeTransformer = null;
        }

        // Retransform modified classes back to their original vanilla bytecodes
        for (Class<?> clazz : inst.getAllLoadedClasses()) {
            String name = clazz.getName();
            if (name.equals("net.minecraft.client.renderer.GameRenderer") ||
                name.equals("net.minecraft.client.gui.Gui") ||
                name.equals("net.minecraft.client.KeyboardHandler")) {
                try {
                    System.out.println("[HaloAgent] Retransforming class back to vanilla: " + name);
                    inst.retransformClasses(clazz);
                } catch (Exception e) {
                    System.err.println("[HaloAgent] Failed to retransform class back: " + name);
                    e.printStackTrace();
                }
            }
        }

        // Reflectively remove from Fabric Loader and reload resource packs
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            if (loader == null || loader == ClassLoader.getSystemClassLoader()) {
                for (Class<?> clazz : inst.getAllLoadedClasses()) {
                    if (clazz.getName().equals("net.minecraft.client.Minecraft")) {
                        loader = clazz.getClassLoader();
                        break;
                    }
                }
            }

            // Close screen if it belongs to our client (e.g. ClickGUI)
            if (loader != null && loader != ClassLoader.getSystemClassLoader()) {
                try {
                    Class<?> mcClass = loader.loadClass("net.minecraft.client.Minecraft");
                    Object mc = mcClass.getMethod("getInstance").invoke(null);
                    java.lang.reflect.Field screenField = mcClass.getField("screen");
                    screenField.setAccessible(true);
                    Object screen = screenField.get(mc);
                    if (screen != null && screen.getClass().getName().startsWith("com.haloclient.client")) {
                        mcClass.getMethod("setScreen", loader.loadClass("net.minecraft.client.gui.screens.Screen")).invoke(mc, (Object) null);
                        System.out.println("[HaloAgent] Force closed active client GUI screen.");
                    }
                } catch (Exception e) {
                    // Ignore screen close errors
                }
            }

            if (loader != null && loader != ClassLoader.getSystemClassLoader()) {
                Class<?> fabricLoaderClass = loader.loadClass("net.fabricmc.loader.impl.FabricLoaderImpl");
                Object fabricLoader = fabricLoaderClass.getField("INSTANCE").get(null);

                java.lang.reflect.Field modsField = fabricLoaderClass.getDeclaredField("mods");
                modsField.setAccessible(true);
                java.util.List<Object> mods = (java.util.List<Object>) modsField.get(fabricLoader);

                java.lang.reflect.Field modMapField = fabricLoaderClass.getDeclaredField("modMap");
                modMapField.setAccessible(true);
                java.util.Map<String, Object> modMap = (java.util.Map<String, Object>) modMapField.get(fabricLoader);

                mods.removeIf(m -> {
                    try {
                        Object meta = m.getClass().getMethod("getMetadata").invoke(m);
                        String id = (String) meta.getClass().getMethod("getId").invoke(meta);
                        return "halo".equals(id);
                    } catch (Exception e) {
                        return false;
                    }
                });
                modMap.remove("halo");

                System.out.println("[HaloAgent] Successfully removed mod 'halo' from Fabric Loader.");
            }
        } catch (Exception e) {
            System.err.println("[HaloAgent] Failed to remove mod from Fabric Loader during uninject:");
            e.printStackTrace();
        }

        initialized = false;
        System.out.println("[HaloAgent] Uninjected successfully!");
    }

    private static byte[] transformGameRenderer(byte[] bytes) {
        ClassReader cr = new ClassReader(bytes);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (name.equals("renderLevel") && descriptor.equals("(Lnet/minecraft/client/DeltaTracker;)V")) {
                    return new MethodVisitor(Opcodes.ASM9, mv) {
                        @Override
                        public void visitInsn(int opcode) {
                            if (opcode == Opcodes.RETURN) {
                                System.out.println("[HaloAgent] Injecting hook into GameRenderer.renderLevel");
                                visitVarInsn(Opcodes.ALOAD, 0);
                                visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
                                visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false);
                                visitMethodInsn(Opcodes.INVOKESTATIC, "com/haloclient/client/agent/HaloAgent", "initializeStandaloneReflective", "(Ljava/lang/ClassLoader;)V", false);

                                visitVarInsn(Opcodes.ALOAD, 0);
                                visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
                                visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false);
                                visitMethodInsn(Opcodes.INVOKESTATIC, "com/haloclient/client/agent/HaloAgent", "updateCaptureReflective", "(Ljava/lang/ClassLoader;)V", false);
                            }
                            super.visitInsn(opcode);
                        }
                    };
                }
                return mv;
            }
        };
        cr.accept(cv, ClassReader.EXPAND_FRAMES);
        return cw.toByteArray();
    }

    private static byte[] transformGui(byte[] bytes) {
        ClassReader cr = new ClassReader(bytes);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (name.equals("extractRenderState") && descriptor.equals("(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V")) {
                    return new MethodVisitor(Opcodes.ASM9, mv) {
                        @Override
                        public void visitInsn(int opcode) {
                            if (opcode == Opcodes.RETURN) {
                                System.out.println("[HaloAgent] Injecting hook into Gui.extractRenderState");
                                visitVarInsn(Opcodes.ALOAD, 0);
                                visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
                                visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false);
                                visitMethodInsn(Opcodes.INVOKESTATIC, "com/haloclient/client/agent/HaloAgent", "initializeStandaloneReflective", "(Ljava/lang/ClassLoader;)V", false);

                                visitVarInsn(Opcodes.ALOAD, 0);
                                visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
                                visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false);
                                visitVarInsn(Opcodes.ALOAD, 1);
                                visitVarInsn(Opcodes.ALOAD, 2);
                                visitMethodInsn(Opcodes.INVOKESTATIC, "com/haloclient/client/agent/HaloAgent", "renderModulesReflective", "(Ljava/lang/ClassLoader;Ljava/lang/Object;Ljava/lang/Object;)V", false);
                            }
                            super.visitInsn(opcode);
                        }
                    };
                }
                return mv;
            }
        };
        cr.accept(cv, ClassReader.EXPAND_FRAMES);
        return cw.toByteArray();
    }

    private static byte[] transformKeyboard(byte[] bytes) {
        ClassReader cr = new ClassReader(bytes);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (name.equals("keyPress") && descriptor.equals("(JILnet/minecraft/client/input/KeyEvent;)V")) {
                    return new MethodVisitor(Opcodes.ASM9, mv) {
                        @Override
                        public void visitCode() {
                            System.out.println("[HaloAgent] Injecting keyPress head hook");
                            visitVarInsn(Opcodes.ALOAD, 0);
                            visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
                            visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false);
                            visitVarInsn(Opcodes.LLOAD, 1);
                            visitVarInsn(Opcodes.ILOAD, 3);
                            visitVarInsn(Opcodes.ALOAD, 4);
                            visitMethodInsn(Opcodes.INVOKESTATIC, "com/haloclient/client/agent/HaloAgent", "onKeyPressReflective", "(Ljava/lang/ClassLoader;JILjava/lang/Object;)V", false);
                            super.visitCode();
                        }
                    };
                }
                return mv;
            }
        };
        cr.accept(cv, ClassReader.EXPAND_FRAMES);
        return cw.toByteArray();
    }

    public static void registerModInFabric(ClassLoader loader) {
        try {
            Class<?> fabricLoaderClass = loader.loadClass("net.fabricmc.loader.impl.FabricLoaderImpl");
            Object fabricLoader = fabricLoaderClass.getField("INSTANCE").get(null);
            
            // Get mods list and modMap
            java.lang.reflect.Field modsField = fabricLoaderClass.getDeclaredField("mods");
            modsField.setAccessible(true);
            java.util.List<Object> mods = (java.util.List<Object>) modsField.get(fabricLoader);
            
            java.lang.reflect.Field modMapField = fabricLoaderClass.getDeclaredField("modMap");
            modMapField.setAccessible(true);
            java.util.Map<String, Object> modMap = (java.util.Map<String, Object>) modMapField.get(fabricLoader);
            
            if (modMap.containsKey("halo")) {
                System.out.println("[HaloAgent] Mod 'halo' already registered in Fabric Loader.");
                return;
            }
            
            // Create a mock ModContainer using Dynamic Proxy
            Class<?> modContainerClass = loader.loadClass("net.fabricmc.loader.api.ModContainer");
            Class<?> modMetadataClass = loader.loadClass("net.fabricmc.loader.api.metadata.ModMetadata");
            
            java.io.File jarFile = getAgentJarFile();
            java.nio.file.Path jarPath = jarFile.toPath();
            java.nio.file.FileSystem jarFs = java.nio.file.FileSystems.newFileSystem(jarPath, (ClassLoader) null);
            java.nio.file.Path rootPath = jarFs.getPath("/");
            
            Object mockMetadata = java.lang.reflect.Proxy.newProxyInstance(
                loader,
                new Class<?>[]{modMetadataClass},
                (proxy, method, args) -> {
                    if (method.getName().equals("getId")) {
                        return "halo";
                    }
                    if (method.getName().equals("getVersion")) {
                        Class<?> versionClass = loader.loadClass("net.fabricmc.loader.api.Version");
                        return java.lang.reflect.Proxy.newProxyInstance(
                            loader,
                            new Class<?>[]{versionClass},
                            (pVersion, mVersion, aVersion) -> {
                                if (mVersion.getName().equals("getFriendlyString")) {
                                    return "1.0.0";
                                }
                                if (mVersion.getName().equals("toString")) {
                                    return "1.0.0";
                                }
                                return null;
                            }
                        );
                    }
                    if (method.getName().equals("getName")) {
                        return "Halo Client";
                    }
                    if (method.getName().equals("getType")) {
                        return "fabric";
                    }
                    if (method.getReturnType().equals(java.util.Collection.class)) {
                        return java.util.Collections.emptyList();
                    }
                    if (method.getReturnType().equals(java.util.Map.class)) {
                        return java.util.Collections.emptyMap();
                    }
                    return null;
                }
            );
            
            Object mockContainer = java.lang.reflect.Proxy.newProxyInstance(
                loader,
                new Class<?>[]{modContainerClass},
                (proxy, method, args) -> {
                    if (method.getName().equals("getMetadata")) {
                        return mockMetadata;
                    }
                    if (method.getName().equals("getRootPaths")) {
                        return java.util.Collections.singletonList(rootPath);
                    }
                    if (method.getName().equals("getRootPath")) {
                        return rootPath;
                    }
                    if (method.getName().equals("findPath")) {
                        String pathStr = (String) args[0];
                        return rootPath.resolve(pathStr);
                    }
                    if (method.getName().equals("getPath")) {
                        String pathStr = (String) args[0];
                        return rootPath.resolve(pathStr);
                    }
                    if (method.getName().equals("getOrigin")) {
                        Class<?> originClass = loader.loadClass("net.fabricmc.loader.api.ModContainer$ModOrigin");
                        return java.lang.reflect.Proxy.newProxyInstance(
                            loader,
                            new Class<?>[]{originClass},
                            (p2, m2, a2) -> {
                                if (m2.getName().equals("getKind")) {
                                    return Enum.valueOf((Class<Enum>) loader.loadClass("net.fabricmc.loader.api.ModContainer$ModOrigin$Kind"), "JAR");
                                }
                                if (m2.getName().equals("getPaths")) {
                                    return java.util.Collections.singletonList(jarPath);
                                }
                                return null;
                            }
                        );
                    }
                    return null;
                }
            );
            
            // Add to Fabric Loader
            mods.add(mockContainer);
            modMap.put("halo", mockContainer);
            System.out.println("[HaloAgent] Mock ModContainer 'halo' successfully registered in Fabric Loader!");
        } catch (Exception e) {
            System.err.println("[HaloAgent] Failed to register mod in Fabric Loader:");
            e.printStackTrace();
        }
    }

    public static void reloadResourcesReflective(ClassLoader loader) {
        try {
            Class<?> mcClass = loader.loadClass("net.minecraft.client.Minecraft");
            Object mc = mcClass.getMethod("getInstance").invoke(null);
            
            String[] methodNames = {"reloadResourcePacks", "reloadResources", "method_1508"};
            for (String name : methodNames) {
                try {
                    java.lang.reflect.Method m = mc.getClass().getMethod(name);
                    System.out.println("[HaloAgent] Triggering resource reload via: " + name);
                    m.invoke(mc);
                    return;
                } catch (NoSuchMethodException e) {
                    // Try next
                }
            }
            System.err.println("[HaloAgent] Could not find resource reload method in Minecraft.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized void initializeStandaloneReflective(ClassLoader loader) {
        if (initialized) return;
        try {
            System.out.println("[HaloAgent] Registering mod in Fabric Loader first...");
            registerModInFabric(loader);

            System.out.println("[HaloAgent] Initializing standalone client reflectively...");
            Class<?> clientClass = loader.loadClass("com.haloclient.client.HaloClient");
            java.lang.reflect.Method initMethod = clientClass.getMethod("initializeStandalone");
            initMethod.invoke(null);
            initialized = true;
            System.out.println("[HaloAgent] Standalone client initialized successfully.");

            System.out.println("[HaloAgent] Triggering resource pack reload to mount assets...");
            reloadResourcesReflective(loader);
        } catch (Exception e) {
            System.err.println("[HaloAgent] Failed to initialize standalone client reflectively:");
            e.printStackTrace();
        }
    }

    public static void renderModulesReflective(ClassLoader loader, Object graphics, Object deltaTracker) {
        try {
            Class<?> clientClass = loader.loadClass("com.haloclient.client.HaloClient");
            java.lang.reflect.Method renderMethod = clientClass.getMethod("renderModules", 
                loader.loadClass("net.minecraft.client.gui.GuiGraphicsExtractor"),
                loader.loadClass("net.minecraft.client.DeltaTracker")
            );
            renderMethod.invoke(null, graphics, deltaTracker);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void updateCaptureReflective(ClassLoader loader) {
        try {
            Class<?> mcClass = loader.loadClass("net.minecraft.client.Minecraft");
            Object mcInstance = mcClass.getMethod("getInstance").invoke(null);
            
            Class<?> captureManagerClass = loader.loadClass("com.haloclient.client.render.CaptureManager");
            captureManagerClass.getMethod("updateCapture", mcClass).invoke(null, mcInstance);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void onKeyPressReflective(ClassLoader loader, long window, int action, Object eventObj) {
        if (action != 1) return; // GLFW_PRESS is 1
        try {
            java.lang.reflect.Method keyMethod = eventObj.getClass().getMethod("key");
            keyMethod.setAccessible(true);
            int key = (Integer) keyMethod.invoke(eventObj);

            Class<?> mcClass = loader.loadClass("net.minecraft.client.Minecraft");
            Object mc = mcClass.getMethod("getInstance").invoke(null);
            
            java.lang.reflect.Field screenField = mcClass.getField("screen");
            screenField.setAccessible(true);
            Object screen = screenField.get(mc);
            if (screen != null) return;

            if (key == 344) { // GLFW_KEY_RIGHT_SHIFT is 344
                Class<?> clickGuiClass = loader.loadClass("com.haloclient.client.gui.click.ClickGUI");
                Object clickGui = clickGuiClass.getConstructor().newInstance();
                java.lang.reflect.Method setScreenMethod = mcClass.getMethod("setScreen", loader.loadClass("net.minecraft.client.gui.screens.Screen"));
                setScreenMethod.invoke(mc, clickGui);
                return;
            }

            Class<?> clientClass = loader.loadClass("com.haloclient.client.HaloClient");
            Object instance = clientClass.getField("INSTANCE").get(null);
            if (instance != null) {
                Object moduleManager = clientClass.getMethod("getModuleManager").invoke(instance);
                java.util.List<?> modules = (java.util.List<?>) moduleManager.getClass().getMethod("getModules").invoke(moduleManager);
                for (Object module : modules) {
                    int moduleKey = (Integer) module.getClass().getMethod("getKey").invoke(module);
                    if (moduleKey == key) {
                        module.getClass().getMethod("toggle").invoke(module);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
