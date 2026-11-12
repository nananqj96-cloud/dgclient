package com.dg.client.render;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.SpriteAtlasTexture;

/**
 * Wraps a VertexConsumerProvider so every emitted quad uses an emissive translucent layer
 * tinted with DGClient's pulsing neon green/white color.
 */
public final class TintingVertexConsumerProvider implements VertexConsumerProvider {
    private final VertexConsumerProvider delegate;
    private final int r;
    private final int g;
    private final int b;
    private final int a;

    public TintingVertexConsumerProvider(VertexConsumerProvider delegate, int argb) {
        this.delegate = delegate;
        this.a = (argb >>> 24) & 0xFF;
        this.r = (argb >>> 16) & 0xFF;
        this.g = (argb >>> 8) & 0xFF;
        this.b = argb & 0xFF;
    }

    @Override
    public VertexConsumer getBuffer(RenderLayer renderLayer) {
        VertexConsumer inner = delegate.getBuffer(RenderLayer.getEntityTranslucentEmissive(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
        return new TintedConsumer(inner, r, g, b, a);
    }

    private record TintedConsumer(VertexConsumer inner, int r, int g, int b, int a) implements VertexConsumer {
        @Override
        public VertexConsumer vertex(float x, float y, float z) {
            inner.vertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            inner.color(r, g, b, a);
            return this;
        }

        @Override
        public VertexConsumer texture(float u, float v) {
            inner.texture(u, v);
            return this;
        }

        @Override
        public VertexConsumer overlay(int u, int v) {
            inner.overlay(u, v);
            return this;
        }

        @Override
        public VertexConsumer light(int u, int v) {
            inner.light(0xF0, 0xF0);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            inner.normal(x, y, z);
            return this;
        }
    }
}
