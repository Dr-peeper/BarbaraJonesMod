package com.barbarajones.v2.quests;

import com.barbarajones.v2.quests.net.QuestNetwork;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The quest module's one and only entry point.
 *
 * <p>The orchestrator calls {@link #init(IEventBus)} from the mod constructor and
 * nothing else. Everything past that point wires itself up:
 * <ul>
 *   <li>{@link QuestRegistry} registers the Codex item on the mod bus.</li>
 *   <li>{@link QuestEvents} is an {@code @Mod.EventBusSubscriber}, so it hooks the
 *       Forge bus - including installing {@link QuestLoader} as a datapack listener -
 *       without a shared file being touched.</li>
 *   <li>{@link com.barbarajones.v2.quests.client.QuestClientSetup} does the same on
 *       the client, guarded by {@code Dist.CLIENT}.</li>
 *   <li>{@link QuestNetwork} registers its packets in common setup below.</li>
 * </ul>
 */
public final class QuestModule {

    public static final Logger LOG = LoggerFactory.getLogger("BarbaraJones/Quests");

    private QuestModule() {
    }

    /** Call once, from the mod constructor, with the MOD event bus. */
    public static void init(IEventBus modBus) {
        QuestRegistry.init(modBus);
        modBus.addListener(QuestModule::commonSetup);
        LOG.info("Quest module armed. Definitions load from data/<namespace>/quests/.");
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(QuestNetwork::register);
    }
}
