package com.vomiter.damagesourceisnotnull;


import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import net.neoforged.neoforgespi.language.IModInfo;

import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;

public final class ModIdResolver {

    private ModIdResolver() {}

    public static String resolveModFromClassName(String className) {
        try {
            Class<?> c = Class.forName(className, false, Thread.currentThread().getContextClassLoader());

            URL locUrl = c.getProtectionDomain() != null && c.getProtectionDomain().getCodeSource() != null
                    ? c.getProtectionDomain().getCodeSource().getLocation()
                    : null;

            if (locUrl == null) return "<unknown>";

            // 1) 先取原始字串（很重要，Forge production 常常不是 file:/...）
            String raw = locUrl.toString();

            // 2) 從 raw 抽出 jar 檔名（最後的 xxx.jar）
            String jarName = extractJarFileName(raw);
            if (jarName == null) return "<unknown>";

            // 3) 用 jarName 去 ModList 比對
            Optional<IModInfo> hit = ModList.get().getModFiles().stream()
                    .flatMap(mf -> mf.getMods().stream().map(mi -> new Object[]{ mi, mf }))
                    .filter(arr -> {
                        var mf = (ModFileInfo) ((Object[])arr)[1];
                        Path p = mf.getFile().getFilePath();
                        if (p == null) return false;
                        String fileName = p.getFileName().toString();
                        return fileName.equalsIgnoreCase(jarName);
                    })
                    .map(arr -> (IModInfo) ((Object[])arr)[0])
                    .findFirst();

            if (hit.isEmpty()) return "<unknown>";
            return hit.get().getModId() + " (" + hit.get().getDisplayName() + ")";
        } catch (Throwable t) {
            return "<unknown>";
        }
    }

    private static String extractJarFileName(String rawUrl) {
        // 解碼 %23 之類
        String s = URLDecoder.decode(rawUrl, StandardCharsets.UTF_8);

        // 常見：jar:file:/.../mod.jar!/ 或 union:/.../mod.jar#196!/ 或 file:/.../mod.jar
        int jarIdx = s.lastIndexOf(".jar");
        if (jarIdx < 0) return null;

        // 往回找上一個 '/' 或 '\\'
        int slash = Math.max(s.lastIndexOf('/', jarIdx), s.lastIndexOf('\\', jarIdx));
        if (slash < 0) return null;

        return s.substring(slash + 1, jarIdx + 4); // 含 .jar
    }
}