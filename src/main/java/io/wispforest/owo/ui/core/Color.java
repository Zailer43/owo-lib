package io.wispforest.owo.ui.core;

import com.google.common.collect.ImmutableMap;
import io.wispforest.owo.ui.parsing.UIModelParsingException;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Node;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

public record Color(float red, float green, float blue, float alpha) implements Animatable<Color> {

    public static final Color BLACK = Color.ofRgb(0);
    public static final Color WHITE = Color.ofRgb(0xFFFFFF);
    public static final Color RED = Color.ofRgb(0xFF0000);
    public static final Color GREEN = Color.ofRgb(0x00FF00);
    public static final Color BLUE = Color.ofRgb(0x0000FF);

    private static final Map<String, Color> NAMED_TEXT_COLORS = Stream.of(Formatting.values())
            .filter(Formatting::isColor)
            .collect(ImmutableMap.toImmutableMap(formatting -> {
                return formatting.getName().toLowerCase(Locale.ROOT).replace("_", "-");
            }, Color::ofFormatting));

    public Color(float red, float green, float blue) {
        this(red, green, blue, 1f);
    }

    public static Color ofArgb(int argb) {
        return new Color(
                ((argb >> 16) & 0xFF) / 255f,
                ((argb >> 8) & 0xFF) / 255f,
                (argb & 0xFF) / 255f,
                (argb >>> 24) / 255f
        );
    }

    public static Color ofRgb(int rgb) {
        return new Color(
                ((rgb >> 16) & 0xFF) / 255f,
                ((rgb >> 8) & 0xFF) / 255f,
                (rgb & 0xFF) / 255f,
                1f
        );
    }

    public static Color ofHsv(float hue, float saturation, float value) {
        // we call .5e-7f the magic "do not turn a hue value of 1f into yellow" constant
        return ofRgb(MathHelper.hsvToRgb(hue - .5e-7f, saturation, value));
    }

    public static Color ofHsv(float hue, float saturation, float value, float alpha) {
        // we call .5e-7f the magic "do not turn a hue value of 1f into yellow" constant
        return ofArgb((int) (alpha * 255) << 24 | MathHelper.hsvToRgb(hue - .5e-7f, saturation, value));
    }

    public static Color ofFormatting(@NotNull Formatting formatting) {
        var colorValue = formatting.getColorValue();
        return ofRgb(colorValue == null ? 0 : colorValue);
    }

    public static Color ofDye(@NotNull DyeColor dyeColor) {
        return ofArgb(dyeColor.getEntityColor());
    }

    /**
     * Generates a random color
     * @apiNote Don't tell glisco about this
     * @author chyzman
     */
    public static Color random() {
        return ofArgb((int) (Math.random() * 0xFFFFFF) | 0xFF000000);
    }

    public int rgb() {
        return (int) (this.red * 255) << 16
                | (int) (this.green * 255) << 8
                | (int) (this.blue * 255);
    }

    public int argb() {
        return (int) (this.alpha * 255) << 24
                | (int) (this.red * 255) << 16
                | (int) (this.green * 255) << 8
                | (int) (this.blue * 255);
    }

    public float[] hsv() {
        float hue, saturation, value;

        float cmax = Math.max(Math.max(this.red, this.green), this.blue);
        float cmin = Math.min(Math.min(this.red, this.green), this.blue);

        value = cmax;
        if (cmax != 0) {
            saturation = (cmax - cmin) / cmax;
        } else {
            saturation = 0;
        }

        if (saturation == 0) {
            hue = 0;
        } else {
            float redc = (cmax - this.red) / (cmax - cmin);
            float greenc = (cmax - this.green) / (cmax - cmin);
            float bluec = (cmax - this.blue) / (cmax - cmin);

            if (this.red == cmax) {
                hue = bluec - greenc;
            } else if (this.green == cmax)
                hue = 2.0f + redc - bluec;
            else {
                hue = 4.0f + greenc - redc;
            }

            hue = hue / 6.0f;
            if (hue < 0) hue = hue + 1.0f;
        }

        return new float[]{hue, saturation, value, this.alpha};
    }

    public String asHexString(boolean includeAlpha) {
        return includeAlpha
                ? String.format("#%08X", this.argb())
                : String.format("#%06X", this.rgb());
    }

    @Override
    public Color interpolate(Color next, float delta) {
        return new Color(
                MathHelper.lerp(delta, this.red, next.red),
                MathHelper.lerp(delta, this.green, next.green),
                MathHelper.lerp(delta, this.blue, next.blue),
                MathHelper.lerp(delta, this.alpha, next.alpha)
        );
    }

    /**
     * Tries to interpret the given node's text content as a color
     * in {@code #RRGGBB} or {@code #AARRGGBB} format, or as
     * the name of a text color
     *
     * @return The parsed color as an unsigned integer
     * @throws UIModelParsingException If the text content does not match
     *                                 the expected color format
     */
    public static Color parse(Node node) {
        var text = node.getTextContent().strip();

        if (!text.startsWith("#")) {
            var color = NAMED_TEXT_COLORS.get(text);
            if (color != null) {
                return color;
            } else {
                throw new UIModelParsingException("Invalid color value '" + text + "', expected hex color of format #RRGGBB or #AARRGGBB or named text color");
            }
        } else {
            if (text.matches("#([A-Fa-f\\d]{2}){3,4}")) {
                return text.length() == 7
                        ? Color.ofRgb(Integer.parseUnsignedInt(text.substring(1), 16))
                        : Color.ofArgb(Integer.parseUnsignedInt(text.substring(1), 16));
            } else {
                throw new UIModelParsingException("Invalid color value '" + text + "', expected hex color of format #RRGGBB or #AARRGGBB or named text color");
            }
        }
    }

    public static int parseAndPack(Node node) {
        return parse(node).argb();
    }
}
