package com.yumesplugins.purehelper;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

/**
 * The Plugin Hub's code rules, checked on every main source file so a change that breaks one fails
 * the build: no unmanaged HTTP (use the injected OkHttpClient), no reflection, no JNI or JNA, no
 * launching processes, no Java serialization, no dynamic class loading, no {@code new Gson()} (use
 * the injected Gson), and no {@code java.awt.Desktop} (use LinkBrowser). Comments and string
 * literals are ignored, so javadoc that explains a rule doesn't trip it.
 */
public class HubRulesTest
{
    /** Set true for a plugin that must never touch the network; then any okhttp3 import fails too. */
    private static final boolean LOCAL_ONLY = false;

    private static final Map<String, Pattern> FORBIDDEN = new LinkedHashMap<>();

    static
    {
        FORBIDDEN.put("unmanaged HTTP", Pattern.compile("HttpURLConnection|java\\.net\\.http|new (okhttp3\\.)?OkHttpClient(\\.Builder)?\\(|\\b(SSL|Server|Datagram|Multicast)?Socket(Channel)?\\b"));
        FORBIDDEN.put("reflection", Pattern.compile("java\\.lang\\.(reflect|invoke)|\\.get(Declared)?(Field|Method|Constructor)s?\\("
            + "|\\.setAccessible\\(|Class\\.forName\\("));
        FORBIDDEN.put("JNI/JNA", Pattern.compile("\\bnative\\s+\\w+.*\\(|com\\.sun\\.jna|System\\.load(Library)?\\("));
        FORBIDDEN.put("processes", Pattern.compile("ProcessBuilder|Runtime\\.getRuntime\\(\\)\\.exec"));
        FORBIDDEN.put("serialization", Pattern.compile("Object(Input|Output)Stream|implements[^{]*\\bSerializable\\b"));
        FORBIDDEN.put("class loading", Pattern.compile("new \\w*ClassLoader\\(|extends \\w*ClassLoader\\b|defineClass\\(|\\.loadClass\\("));
        FORBIDDEN.put("new Gson", Pattern.compile("new (com\\.google\\.gson\\.)?Gson(Builder)?\\("));
        FORBIDDEN.put("Desktop", Pattern.compile("java\\.awt\\.Desktop|\\bDesktop\\.getDesktop\\("));
        if (LOCAL_ONLY)
        {
            FORBIDDEN.put("network", Pattern.compile("import (okhttp3|java\\.net|javax\\.net)\\."));
        }
    }

    @Test
    public void mainSourcesKeepThePluginHubRules() throws IOException
    {
        Path main = Paths.get("src", "main", "java");
        assertTrue("run from the project directory", Files.isDirectory(main));
        List<Path> sources;
        try (Stream<Path> walk = Files.walk(main))
        {
            sources = walk.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList());
        }
        assertTrue("found the sources", !sources.isEmpty());
        List<String> hits = new ArrayList<>();
        for (Path source : sources)
        {
            String code = codeOnly(new String(Files.readAllBytes(source), StandardCharsets.UTF_8));
            for (Map.Entry<String, Pattern> rule : FORBIDDEN.entrySet())
            {
                if (rule.getValue().matcher(code).find())
                {
                    hits.add(source + ": " + rule.getKey());
                }
            }
        }
        assertTrue("Hub rules broken: " + hits, hits.isEmpty());
    }

    @Test
    public void eachRuleCatchesWhatItIsFor()
    {
        // So a rule that silently matches nothing can't pass the check above.
        Map<String, String> samples = new LinkedHashMap<>();
        samples.put("unmanaged HTTP", "OkHttpClient client = new OkHttpClient();");
        samples.put("reflection", "field.setAccessible(true);");
        samples.put("JNI/JNA", "private native int peek(long address);");
        samples.put("processes", "new ProcessBuilder(\"cmd\").start();");
        samples.put("serialization", "new ObjectOutputStream(out);");
        samples.put("class loading", "new URLClassLoader(urls);");
        samples.put("new Gson", "Gson gson = new Gson();");
        samples.put("Desktop", "Desktop.getDesktop().browse(uri);");
        for (Map.Entry<String, String> sample : samples.entrySet())
        {
            assertTrue(sample.getKey(), FORBIDDEN.get(sample.getKey()).matcher(codeOnly(sample.getValue())).find());
        }
        assertTrue("OkHttpClient.Builder",
            FORBIDDEN.get("unmanaged HTTP").matcher("OkHttpClient c = new OkHttpClient.Builder().build();").find());
        assertTrue("GsonBuilder", FORBIDDEN.get("new Gson").matcher("Gson g = new GsonBuilder().create();").find());
        assertTrue("SSLSocket", FORBIDDEN.get("unmanaged HTTP").matcher("SSLSocket s = factory.createSocket();").find());
        assertTrue("public reflection",
            FORBIDDEN.get("reflection").matcher("Foo.class.getMethod(\"x\").invoke(foo);").find());
        assertTrue("MethodHandles", FORBIDDEN.get("reflection").matcher("java.lang.invoke.MethodHandles.lookup();").find());
        // A quote inside a comment, or a comment marker inside a string, must not hide the code after it.
        assertTrue("quote in comment",
            FORBIDDEN.get("processes").matcher(codeOnly("// say \"hi\nnew ProcessBuilder();\n// \"")).find());
        assertTrue("comment marker in string",
            FORBIDDEN.get("processes").matcher(codeOnly("String s = \"/*\";\nnew ProcessBuilder();\n/* */")).find());
        assertTrue("fully qualified OkHttpClient",
            FORBIDDEN.get("unmanaged HTTP").matcher("okhttp3.OkHttpClient c = new okhttp3.OkHttpClient();").find());
    }

    @Test
    public void allowedPatternsPass()
    {
        // The injected client and its builder are how the Hub wants HTTP done.
        String[] allowed = {
            "@Inject private OkHttpClient okHttpClient;",
            "OkHttpClient client = okHttpClient.newBuilder().build();",
            "clientThread.invoke(this::run);",
            "getClass().getClassLoader().getResourceAsStream(\"rules.json\");",
            "// never call new Gson() here",
            "String s = \"\"\"\n    new Gson()\n    \"\"\";",
            "WebSocket ws = okHttpClient.newWebSocket(request, listener);",
        };
        for (String line : allowed)
        {
            for (Map.Entry<String, Pattern> rule : FORBIDDEN.entrySet())
            {
                assertFalse(rule.getKey() + " flagged: " + line, rule.getValue().matcher(codeOnly(line)).find());
            }
        }
    }

    /** Comments, text blocks, strings and char literals, matched in one pass so none can hide inside another. */
    private static final Pattern NOT_CODE = Pattern.compile(
        "(?s)/\\*.*?\\*/|//[^\n]*|\"\"\".*?\"\"\"|\"(?:[^\"\\\\\n]|\\\\.)*\"|'(?:[^'\\\\\n]|\\\\.)*'");

    /** The source without comments and with every string or char literal emptied. */
    private static String codeOnly(String source)
    {
        return NOT_CODE.matcher(source).replaceAll(m -> m.group().startsWith("/") ? " " : "\"\"");
    }
}
