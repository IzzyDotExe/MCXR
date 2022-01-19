package net.sorenon.mcxr.play.rendering;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.sorenon.mcxr.play.mixin.accessor.FramebufferAcc;
import org.lwjgl.opengles.GLES32;

import java.nio.IntBuffer;

import static org.lwjgl.opengles.GLES32.GL_FRAMEBUFFER;
import static org.lwjgl.opengles.GLES32.*;

/*
The framebuffer class is most likely the largest cause of compatibility issues between rendering mods and the game itself.
This is due to Minecraft and these mods expecting the framebuffer to be the same size between frames (excluding after a resize)
they are designed with a constantly sized framebuffer in mind despite the fact that in an XR environment the size of the
framebuffer can change between draws in the same frame

These are the methods that i can think of to deal with this:
1. Redesign each mod to be able to handle multiple sizes of framebuffer
2. Assume all swapchain images are the same size
3. Resize the framebuffer between draws if needed
4. Don't do anything and let the world burn
5. Allocate each framebuffer according to the largest swapchain image and then just change
the size of the viewport rather than the size of the framebuffer

And my thoughts on each:
1. God no
2. Easiest solution and works well with iris
3. Also an easy solution and only effects a small demographic of players
4. Eh maybe
5. Difficult but could work

Method 3 has been implemented
If method 3 has too great of a performance issue (which i doubt) i will look at method 5
 */

/**
 * XrFramebuffer is a framebuffer which accepts a color texture for rendering to rather than creating its own
 * TODO accept depth textures as well
 */
public class XrFramebuffer extends TextureTarget {

    public XrFramebuffer(int width, int height) {
        super(width, height, true, Minecraft.ON_OSX);
    }

    @Override
    public void createBuffers(int width, int height, boolean getError) {
        RenderSystem.assertOnRenderThreadOrInit();
        int i = RenderSystem.maxSupportedTextureSize();
        if (width > 0 && width <= i && height > 0 && height <= i) {
            this.viewWidth = width;
            this.viewHeight = height;
            this.width = width;
            this.height = height;
            this.frameBufferId = GLES32.glGenFramebuffers();
//            this.colorAttachment = TextureUtil.generateTextureId();

            this.setFilterMode(9728);
//            GlStateManager._bindTexture(this.colorAttachment);
//            GlStateManager._texParameter(3553, 10242, 33071);
//            GlStateManager._texParameter(3553, 10243, 33071);
//            GlStateManager._texImage2D(3553, 0, 32856, this.textureWidth, this.textureHeight, 0, 6408, 5121, (IntBuffer)null);
            GLES32.glBindBuffer(36160, this.frameBufferId);
//            GlStateManager._glFramebufferTexture2D(36160, 36064, 3553, this.colorAttachment, 0);

            this.checkStatus();
            this.clear(getError);
            this.unbindRead();
        } else {
            throw new IllegalArgumentException("Window " + width + "x" + height + " size out of bounds (max. size: " + i + ")");
        }
    }

    public void setColorAttachment(int colorAttachment) {
        ((FramebufferAcc) this).colorAttachment(colorAttachment);
        GLES32.glBindFramebuffer(GLES32.GL_FRAMEBUFFER, frameBufferId);
        GLES32.glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, colorAttachment, 0);
    }
}
