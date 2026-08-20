package com.barbarajones.v2.village.menu;

import com.barbarajones.v2.village.KraveProfession;
import com.barbarajones.v2.village.KraveVillagerEntity;
import com.barbarajones.v2.village.VillageOffer;
import com.barbarajones.v2.village.VillageRegistry;
import com.barbarajones.v2.village.VillageTrades;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The Krave trading menu.
 *
 * <p>A real custom menu rather than a reuse of vanilla's {@code MerchantMenu},
 * because the screen has to show three things vanilla has no room for: the
 * villager's trade level, how much Krave it has been fed, and which offers are
 * still locked behind a higher level. Those are the whole point of the feeding
 * loop, so they cannot live in a tooltip.
 *
 * <h2>Slot map</h2>
 * <pre>
 *   0  payment A      (172, 66)
 *   1  payment B      (198, 66)
 *   2  result         (242, 66)   read-only; taking from it performs the trade
 *   3..29   player inventory
 *   30..38  player hotbar
 * </pre>
 *
 * <h2>Where the offer list lives</h2>
 * On the server the list <em>is</em> the villager's. On the client it arrives in
 * {@link com.barbarajones.v2.village.net.PacketVillageOffers} and is stashed here.
 * Selection is client-initiated but server-authoritative: clicking a row sends an
 * index, the server validates it against its own list and fills the result slot
 * itself. A client that could name an offer the server does not have would be a
 * duplication bug, so the index is the only thing that ever crosses.
 *
 * <h2>Data slots</h2>
 * Six ints (level, XP, XP-to-next, selection, Krave fed, profession) ride the
 * vanilla container-data sync, refreshed from the villager in
 * {@link #broadcastChanges()}. That gets the XP bar and the level badge updating
 * live during a trade without a bespoke packet for each.
 */
public class KraveTradeMenu extends AbstractContainerMenu {

    public static final int SLOT_PAY_A = 0;
    public static final int SLOT_PAY_B = 1;
    public static final int SLOT_RESULT = 2;
    private static final int INVENTORY_START = 3;
    private static final int INVENTORY_END = 39;

    /** Indices into the {@link SimpleContainerData}. */
    public static final int DATA_LEVEL = 0;
    public static final int DATA_XP = 1;
    public static final int DATA_XP_NEXT = 2;
    public static final int DATA_SELECTED = 3;
    public static final int DATA_KRAVE_FED = 4;
    public static final int DATA_PROFESSION = 5;
    private static final int DATA_COUNT = 6;

    @Nullable
    private final KraveVillagerEntity villager;
    private final Player player;
    private final SimpleContainerData data = new SimpleContainerData(DATA_COUNT);

    private final SimpleContainer payment = new SimpleContainer(2) {
        @Override
        public void setChanged() {
            super.setChanged();
            KraveTradeMenu.this.slotsChanged(this);
        }
    };
    private final SimpleContainer result = new SimpleContainer(1);

    /** Client-side mirror of the villager's offers. Empty on the server. */
    private List<VillageOffer> clientOffers = Collections.emptyList();

    private int selected = -1;

    /** Server constructor - called from {@code KraveVillagerEntity.openTradeScreen}. */
    public KraveTradeMenu(int id, Inventory inventory, @Nullable KraveVillagerEntity villager) {
        super(VillageRegistry.KRAVE_TRADE.get(), id);
        this.villager = villager;
        this.player = inventory.player;
        buildSlots(inventory);
        addDataSlots(this.data);
        refreshData();
    }

    /**
     * Client constructor. Resolves the villager from the entity id written into the
     * open packet; a null result is tolerated (the entity may not be tracked yet)
     * and the screen simply draws from the synced data until the offers packet
     * lands.
     */
    public KraveTradeMenu(int id, Inventory inventory, int entityId) {
        this(id, inventory, resolve(inventory, entityId));
    }

    @Nullable
    private static KraveVillagerEntity resolve(Inventory inventory, int entityId) {
        Entity entity = inventory.player.level().getEntity(entityId);
        return entity instanceof KraveVillagerEntity villager ? villager : null;
    }

    private void buildSlots(Inventory inventory) {
        addSlot(new Slot(this.payment, 0, 172, 66));
        addSlot(new Slot(this.payment, 1, 198, 66));
        addSlot(new TradeResultSlot(this.result, 242, 66));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 57 + col * 18, 150 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 57 + col * 18, 210));
        }
    }

    // ---- offers --------------------------------------------------------------

    @Nullable
    public KraveVillagerEntity getVillager() {
        return this.villager;
    }

    /** Server: the villager's live list. Client: the last synced mirror. */
    public List<VillageOffer> getOffers() {
        if (this.villager != null && !this.player.level().isClientSide) {
            return this.villager.getOffers();
        }
        return this.clientOffers;
    }

    /** Called by the offers packet handler. Client only. */
    public void setClientOffers(List<VillageOffer> offers, int level, int xp, int kraveFed, int profession) {
        this.clientOffers = new ArrayList<>(offers);
        this.data.set(DATA_LEVEL, level);
        this.data.set(DATA_XP, xp);
        this.data.set(DATA_XP_NEXT, VillageTrades.xpForLevel(Math.min(VillageTrades.MAX_LEVEL, level + 1)));
        this.data.set(DATA_KRAVE_FED, kraveFed);
        this.data.set(DATA_PROFESSION, profession);
        if (this.selected >= this.clientOffers.size()) {
            this.selected = -1;
        }
    }

    public int getSelected() {
        return this.data.get(DATA_SELECTED) - 1;
    }

    @Nullable
    public VillageOffer selectedOffer() {
        int index = this.player.level().isClientSide ? getSelected() : this.selected;
        List<VillageOffer> offers = getOffers();
        return index >= 0 && index < offers.size() ? offers.get(index) : null;
    }

    /**
     * Server-side selection. Bounds-checked against the server's own list, then the
     * result slot is recomputed. Also usable client-side for a snappy highlight
     * before the round trip lands.
     */
    public void selectOffer(int index) {
        List<VillageOffer> offers = getOffers();
        this.selected = index >= 0 && index < offers.size() ? index : -1;
        this.data.set(DATA_SELECTED, this.selected + 1);
        if (!this.player.level().isClientSide) {
            updateResult();
            broadcastChanges();
        }
    }

    public int getTradeLevel() {
        return Math.max(1, this.data.get(DATA_LEVEL));
    }

    public int getTradeXp() {
        return this.data.get(DATA_XP);
    }

    public int getXpForNextLevel() {
        return this.data.get(DATA_XP_NEXT);
    }

    public int getKraveFed() {
        return this.data.get(DATA_KRAVE_FED);
    }

    public KraveProfession getProfession() {
        return KraveProfession.byOrdinal(this.data.get(DATA_PROFESSION));
    }

    // ---- trade mechanics -----------------------------------------------------

    @Override
    public void slotsChanged(Container container) {
        if (container == this.payment && !this.player.level().isClientSide) {
            updateResult();
        }
        super.slotsChanged(container);
    }

    /**
     * Fills or clears the result slot from the selected offer. Server only - the
     * client sees the outcome through normal slot sync, which is what keeps a
     * modified client from inventing a result.
     */
    private void updateResult() {
        VillageOffer offer = selectedOffer();
        if (offer == null || offer.isOutOfStock()
                || !offer.satisfiedBy(this.payment.getItem(0), this.payment.getItem(1))) {
            this.result.setItem(0, ItemStack.EMPTY);
            return;
        }
        this.result.setItem(0, offer.result().copy());
    }

    /**
     * Runs one trade. Called from the result slot the moment the player takes the
     * output, which is the only place a trade is ever performed - there is no other
     * path that consumes the payment, so there is no second path to get wrong.
     */
    private void performTrade(Player taker) {
        VillageOffer offer = selectedOffer();
        if (offer == null || this.player.level().isClientSide) {
            return;
        }
        ItemStack payA = this.payment.getItem(0);
        ItemStack payB = this.payment.getItem(1);
        if (!offer.satisfiedBy(payA, payB)) {
            return;
        }
        offer.take(payA, payB);
        this.payment.setItem(0, payA.isEmpty() ? ItemStack.EMPTY : payA);
        this.payment.setItem(1, payB.isEmpty() ? ItemStack.EMPTY : payB);

        if (this.villager != null) {
            this.villager.notifyTradeCompleted(offer, taker);
            if (taker instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                this.villager.syncOffersTo(serverPlayer);
            }
        }
        refreshData();
        updateResult();
        broadcastChanges();
    }

    private void refreshData() {
        if (this.villager == null || this.player.level().isClientSide) {
            return;
        }
        int level = this.villager.getTradeLevel();
        this.data.set(DATA_LEVEL, level);
        this.data.set(DATA_XP, this.villager.getTradeXp());
        this.data.set(DATA_XP_NEXT,
                VillageTrades.xpForLevel(Math.min(VillageTrades.MAX_LEVEL, level + 1)));
        this.data.set(DATA_SELECTED, this.selected + 1);
        this.data.set(DATA_KRAVE_FED, this.villager.getKraveFed());
        this.data.set(DATA_PROFESSION, this.villager.getProfession().ordinal());
    }

    @Override
    public void broadcastChanges() {
        refreshData();
        super.broadcastChanges();
    }

    // ---- plumbing ------------------------------------------------------------

    @Override
    public ItemStack quickMoveStack(Player taker, int index) {
        ItemStack carried = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return carried;
        }
        ItemStack stack = slot.getItem();
        carried = stack.copy();

        if (index == SLOT_RESULT) {
            if (!moveItemStackTo(stack, INVENTORY_START, INVENTORY_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, carried);
        } else if (index == SLOT_PAY_A || index == SLOT_PAY_B) {
            if (!moveItemStackTo(stack, INVENTORY_START, INVENTORY_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, SLOT_PAY_A, SLOT_RESULT, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == carried.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(taker, stack);
        return carried;
    }

    @Override
    public boolean stillValid(Player candidate) {
        if (this.villager == null) {
            // Client side before the entity is tracked. Refusing here would close
            // the screen the frame it opened.
            return this.player.level().isClientSide;
        }
        return this.villager.isAlive() && candidate.distanceToSqr(this.villager) < 64.0D;
    }

    @Override
    public void removed(Player leaver) {
        super.removed(leaver);
        if (this.villager != null && !this.player.level().isClientSide) {
            this.villager.setTradingPlayer(null);
        }
        if (!this.player.level().isClientSide) {
            // Anything left in the payment slots goes back to the player rather
            // than into the void - the single most complained-about container bug
            // there is.
            clearContainer(leaver, this.payment);
        }
    }

    /**
     * The output. Read-only, and taking from it is the one and only thing that
     * performs a trade.
     */
    private class TradeResultSlot extends Slot {

        TradeResultSlot(Container container, int x, int y) {
            super(container, 0, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public void onTake(Player taker, ItemStack taken) {
            performTrade(taker);
            super.onTake(taker, taken);
        }
    }
}
