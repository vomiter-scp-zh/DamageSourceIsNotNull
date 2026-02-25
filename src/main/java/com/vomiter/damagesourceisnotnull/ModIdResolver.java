package com.vomiter.damagesourceisnotnull;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;

import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;

public final class ModIdResolver {
    private ModIdResolver() {}

    public static String resolveModFromClassName(String className) {
        try {
            Class<?> c = Class.forName(className, false, Thread.currentThread().getContextClassLoader());

            URL locUrl = (c.getProtectionDomain() != null && c.getProtectionDomain().getCodeSource() != null)
                    ? c.getProtectionDomain().getCodeSource().getLocation()
                    : null;

            if (locUrl == null) return "<unknown>";

            Path classPath = urlToPathSafe(locUrl);
            if (classPath == null) return "<unknown>";

            ModContainer hit = findOwnerMod(classPath, FabricLoader.getInstance().getAllMods());
            if (hit == null) return "<unknown>";

            ModMetadata meta = hit.getMetadata();
            return meta.getId() + " (" + meta.getName() + ")";
        } catch (Throwable t) {
            return "<unknown>";
        }
    }

    private static ModContainer findOwnerMod(Path classPath, Collection<ModContainer> mods) {
        // 1) 先嘗試「直接 Path 命中」：最可靠（特別是 jar / folder）
        for (ModContainer mod : mods) {
            try {
                // origin paths：對應「mod jar/folder 本體」的路徑集合（PATH kind 才支援） :contentReference[oaicite:1]{index=1}
                for (Path p : mod.getOrigin().getPaths()) {
                    if (pathEqualsLoose(p, classPath)) return mod;
                }
            } catch (UnsupportedOperationException ignored) {
                // NESTED / BUILTIN 等 kind 可能不支援 getPaths()
            } catch (Throwable ignored) {}
        }

        // 2) 再用 root paths（dev 環境可能有多個 root path） :contentReference[oaicite:2]{index=2}
        for (ModContainer mod : mods) {
            try {
                for (Path p : mod.getRootPaths()) {
                    if (pathEqualsLoose(p, classPath)) return mod;
                }
            } catch (Throwable ignored) {}
        }

        // 3) 最後 fallback：用檔名比對（等價於你原本抓 jarName）
        String fileName = safeFileName(classPath);
        if (fileName == null) return null;

        for (ModContainer mod : mods) {
            try {
                for (Path p : mod.getOrigin().getPaths()) {
                    String fn = safeFileName(p);
                    if (fn != null && fn.equalsIgnoreCase(fileName)) return mod;
                }
            } catch (Throwable ignored) {}
        }
        for (ModContainer mod : mods) {
            try {
                for (Path p : mod.getRootPaths()) {
                    String fn = safeFileName(p);
                    if (fn != null && fn.equalsIgnoreCase(fileName)) return mod;
                }
            } catch (Throwable ignored) {}
        }

        return null;
    }

    private static boolean pathEqualsLoose(Path a, Path b) {
        if (a == null || b == null) return false;
        // 盡量 normalize；不要強制 toRealPath()，因為 jar filesystem 或虛擬 path 可能會炸
        Path an = a.normalize();
        Path bn = b.normalize();
        if (an.equals(bn)) return true;

        // 某些情況 CodeSource 可能指向「目錄」而 mod origin/root 指向 jar 或相反
        // 用 endsWith 做保守 match（例如 .../mod.jar vs .../mod.jar!/）
        try {
            return bn.endsWith(an) || an.endsWith(bn);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static String safeFileName(Path p) {
        try {
            Path fn = p.getFileName();
            return fn != null ? fn.toString() : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static Path urlToPathSafe(URL url) {
        try {
            // CodeSource 常見是 file:/.../xxx.jar 或 file:/.../classes/...
            URI uri = url.toURI();
            return Path.of(uri);
        } catch (Throwable t) {
            return null;
        }
    }
}