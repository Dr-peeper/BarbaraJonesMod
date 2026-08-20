package com.barbarajones.v2.mobs.client;

import com.barbarajones.v2.mobs.entity.projectile.KraveShardEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

/** Renders exactly as the krave_shard item, spinning through the air - the same trick vanilla uses for snowballs/eggs. */
public class KraveShardRenderer extends ThrownItemRenderer<KraveShardEntity> {

    public KraveShardRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, 1.0F, true);
    }
}
