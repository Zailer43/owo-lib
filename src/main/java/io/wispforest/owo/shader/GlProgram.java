package io.wispforest.owo.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import io.wispforest.owo.mixin.shader.ShaderProgramAccessor;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A simple wrapper around Minecraft's built-in core shaders. In order to load and use
 * a custom shader from your resources, place it in {@code assets/<mod id>/shaders/core/}.
 * You can look up the required files and their format on <a href="https://minecraft.fandom.com/wiki/Shaders">the Minecraft Wiki</a>
 * <p>
 * This wrapper fully supports custom uniforms. If you require any, extend this class, then grab the
 * uniforms via {@link #findUniform(String)} inside your {@link #setup()} override and store them in
 * fields. This method gets executed once the actual shader program has been compiled and
 * linked, ready for use. Look at {@link BlurProgram} for reference
 * <p>
 * GlPrograms automatically register themselves for loading in the constructor - as such,
 * some caution on when and where the constructor is invoked is advisable. Ideally, store
 * and initialize programs in static fields of your client initializer
 */
public class GlProgram {

    private static final List<Pair<Function<ResourceFactory, ShaderProgram>, Consumer<ShaderProgram>>> REGISTERED_PROGRAMS = new ArrayList<>();

    /**
     * The actual Minecraft shader program
     * which is represented and wrapped by this
     * GlProgram instance
     */
    protected ShaderProgram backingProgram;

    public GlProgram(Identifier id, VertexFormat vertexFormat) {
        REGISTERED_PROGRAMS.add(new Pair<>(
                resourceFactory -> {
                    try {
                        return new ShaderProgram(resourceFactory, id.toString(), vertexFormat);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to initialized owo shader program", e);
                    }
                },
                program -> {
                    this.backingProgram = program;
                    this.setup();
                }
        ));
    }

    /**
     * Bind this program and execute
     * potential preparation work
     * <p>
     * <b>Note:</b> Custom implementations may very well have
     * additional setup methods that must be run prior to
     * invoking {@code use()}
     */
    public void use() {
        RenderSystem.setShader(() -> this.backingProgram);
    }

    protected void setup() {}

    /**
     * Get the {@link GlUniform} generated by the game for
     * the uniform of the given name
     *
     * @return The corresponding {@link GlUniform} instance for updating
     * the value of the uniform, or {@code null} if no such uniform exists
     */
    protected @Nullable GlUniform findUniform(String name) {
        return ((ShaderProgramAccessor) this.backingProgram).owo$getLoadedUniforms().get(name);
    }

    @ApiStatus.Internal
    public static void forEachProgram(Consumer<Pair<Function<ResourceFactory, ShaderProgram>, Consumer<ShaderProgram>>> loader) {
        REGISTERED_PROGRAMS.forEach(loader);
    }
}
