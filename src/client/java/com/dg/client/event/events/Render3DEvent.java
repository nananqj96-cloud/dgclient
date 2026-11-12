package com.dg.client.event.events;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import org.joml.Matrix4f;

public final class Render3DEvent {
    private final MatrixStack matrices;
    private final Matrix4f positionMatrix;
    private final Matrix4f projectionMatrix;
    private final Camera camera;
    private final Frustum frustum;
    private final ClientWorld world;
    private final float tickDelta;

    public Render3DEvent(
            MatrixStack matrices,
            Matrix4f positionMatrix,
            Matrix4f projectionMatrix,
            Camera camera,
            Frustum frustum,
            ClientWorld world,
            float tickDelta
    ) {
        this.matrices = matrices;
        this.positionMatrix = positionMatrix;
        this.projectionMatrix = projectionMatrix;
        this.camera = camera;
        this.frustum = frustum;
        this.world = world;
        this.tickDelta = tickDelta;
    }

    public MatrixStack getMatrices() {
        return matrices;
    }

    public Matrix4f getPositionMatrix() {
        return positionMatrix;
    }

    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }

    public Camera getCamera() {
        return camera;
    }

    public Frustum getFrustum() {
        return frustum;
    }

    public ClientWorld getWorld() {
        return world;
    }

    public float getTickDelta() {
        return tickDelta;
    }
}
