package com.barbarajones.v2.quests.net;

import com.barbarajones.v2.quests.Quest;
import com.barbarajones.v2.quests.QuestChapter;
import com.barbarajones.v2.quests.QuestFile;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The whole quest graph, server to client.
 *
 * <p>Sent once on login and once after every {@code /reload}. Definitions are static
 * and shared by everyone, so there is no reason to re-send them when a counter
 * ticks - that is what {@link S2CQuestState} is for. Splitting the two is what keeps
 * a large quest book from putting a definition dump on the wire every time a player
 * picks something up.
 */
public class S2CQuestDefs {

    public final QuestFile file;

    public S2CQuestDefs(QuestFile file) {
        this.file = file;
    }

    public static void encode(S2CQuestDefs msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.file.orderedChapters().size());
        for (QuestChapter chapter : msg.file.orderedChapters()) {
            chapter.encode(buf);
        }
        buf.writeVarInt(msg.file.size());
        for (Quest quest : msg.file.allQuests()) {
            quest.encode(buf);
        }
    }

    public static S2CQuestDefs decode(FriendlyByteBuf buf) {
        int chapterCount = buf.readVarInt();
        Map<ResourceLocation, QuestChapter> chapters = new LinkedHashMap<>();
        for (int i = 0; i < chapterCount; i++) {
            QuestChapter chapter = QuestChapter.decode(buf);
            chapters.put(chapter.id, chapter);
        }
        int questCount = buf.readVarInt();
        Map<ResourceLocation, Quest> quests = new LinkedHashMap<>();
        for (int i = 0; i < questCount; i++) {
            Quest quest = Quest.decode(buf);
            quests.put(quest.id, quest);
        }
        return new S2CQuestDefs(new QuestFile(chapters, quests));
    }

    public static void handle(S2CQuestDefs msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.barbarajones.v2.quests.client.ClientQuests.acceptDefs(msg.file)));
        ctx.get().setPacketHandled(true);
    }
}
