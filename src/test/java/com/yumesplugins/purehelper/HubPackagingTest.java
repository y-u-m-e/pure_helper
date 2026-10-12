package com.yumesplugins.purehelper;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import javax.imageio.ImageIO;

import org.junit.Test;

/**
 * The Plugin Hub packager's checks on files outside the code, run here so a broken listing shows up
 * in the build instead of on the Hub PR. Limits are the packager's own (plugin-hub
 * package/.../packager/Plugin.java). Paths are relative to the project directory, where Gradle
 * runs tests.
 */
public class HubPackagingTest
{
    private static final long ICON_MAX_BYTES = 256 * 1024;
    /** The packager rejects more than 50x100 pixels of area; 48x72 is the recommended size. */
    private static final int ICON_MAX_AREA = 50 * 100;

    @Test
    public void theIconIfPresentPassesThePackager() throws IOException
    {
        // The icon is optional; without one the plugin is listed with no picture.
        Path icon = Paths.get("icon.png");
        if (!Files.exists(icon))
        {
            return;
        }
        assertTrue("icon.png is above the packager's 256KiB limit", Files.size(icon) <= ICON_MAX_BYTES);
        BufferedImage image = ImageIO.read(icon.toFile());
        assertNotNull("icon.png is not an image ImageIO can read", image);
        assertTrue("icon.png is too high-resolution. It should be 48x72 px (is "
                + image.getWidth() + "x" + image.getHeight() + ")",
            image.getWidth() * image.getHeight() <= ICON_MAX_AREA);
    }

    @Test
    public void thePropertiesNameAPluginClassThatExists() throws IOException
    {
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(Paths.get("runelite-plugin.properties")))
        {
            properties.load(in);
        }
        String displayName = properties.getProperty("displayName");
        assertTrue("displayName is set", displayName != null && !displayName.trim().isEmpty());
        String plugins = properties.getProperty("plugins");
        assertNotNull("plugins= is set", plugins);
        for (String className : plugins.split(","))
        {
            Path source = Paths.get("src", "main", "java", className.trim().replace('.', '/') + ".java");
            assertTrue("plugins= names " + className.trim() + " but " + source + " does not exist", Files.isRegularFile(source));
        }
    }

    @Test
    public void thereIsNoServiceLoaderFile()
    {
        // The Hub loads plugins itself and rejects a jar that registers one this way.
        assertFalse(Files.exists(Paths.get("src", "main", "resources", "META-INF", "services",
            "net.runelite.client.plugins.Plugin")));
    }
}
