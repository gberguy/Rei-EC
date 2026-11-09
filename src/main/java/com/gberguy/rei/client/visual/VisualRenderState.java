package com.gberguy.rei.client.visual;

import java.nio.FloatBuffer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;

final class VisualRenderState implements AutoCloseable {
    private final boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
    private final boolean lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
    private final boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
    private final boolean alpha = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
    private final boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
    private final boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
    private final int depthFunction = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
    private final int alphaFunction = GL11.glGetInteger(GL11.GL_ALPHA_TEST_FUNC);
    private final float alphaReference = GL11.glGetFloat(GL11.GL_ALPHA_TEST_REF);
    private final int sourceRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
    private final int destRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
    private final int sourceAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
    private final int destAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
    private final int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
    private final int matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
    private final float lightX = OpenGlHelper.lastBrightnessX, lightY = OpenGlHelper.lastBrightnessY;
    private final FloatBuffer color = BufferUtils.createFloatBuffer(16);
    private final int baseTexture, lightTexture;
    private final boolean baseEnabled, lightEnabled;

    VisualRenderState() {
        GL11.glGetFloat(GL11.GL_CURRENT_COLOR, color);
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        baseTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        baseEnabled = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        lightTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        lightEnabled = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        GlStateManager.matrixMode(GL11.GL_TEXTURE);
        GlStateManager.pushMatrix();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    @Override
    public void close() {
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.popMatrix();
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.matrixMode(GL11.GL_TEXTURE);
        GlStateManager.popMatrix();
        GlStateManager.bindTexture(lightTexture);
        if (lightEnabled) GlStateManager.enableTexture2D(); else GlStateManager.disableTexture2D();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY);
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.bindTexture(baseTexture);
        if (baseEnabled) GlStateManager.enableTexture2D(); else GlStateManager.disableTexture2D();
        if (blend) GlStateManager.enableBlend(); else GlStateManager.disableBlend();
        if (lighting) GlStateManager.enableLighting(); else GlStateManager.disableLighting();
        if (cull) GlStateManager.enableCull(); else GlStateManager.disableCull();
        if (alpha) GlStateManager.enableAlpha(); else GlStateManager.disableAlpha();
        if (depth) GlStateManager.enableDepth(); else GlStateManager.disableDepth();
        GlStateManager.depthMask(depthMask);
        GlStateManager.depthFunc(depthFunction);
        GlStateManager.alphaFunc(alphaFunction, alphaReference);
        GlStateManager.tryBlendFuncSeparate(sourceRgb, destRgb, sourceAlpha, destAlpha);
        GlStateManager.color(color.get(0), color.get(1), color.get(2), color.get(3));
        GlStateManager.matrixMode(matrixMode);
        GlStateManager.setActiveTexture(active);
    }
}
