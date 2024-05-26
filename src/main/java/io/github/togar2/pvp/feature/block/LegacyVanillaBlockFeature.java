package io.github.togar2.pvp.feature.block;

import io.github.togar2.pvp.feature.CombatFeature;
import io.github.togar2.pvp.feature.CombatSetup;
import io.github.togar2.pvp.feature.EntityInstanceFeature;
import io.github.togar2.pvp.feature.item.ItemDamageFeature;
import io.github.togar2.pvp.utils.CombatVersion;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.item.ItemUpdateStateEvent;
import net.minestom.server.event.player.*;
import net.minestom.server.event.trait.EntityInstanceEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.tag.Tag;

public class LegacyVanillaBlockFeature extends VanillaBlockFeature
		implements LegacyBlockFeature, EntityInstanceFeature, CombatFeature {
	public static final Tag<Long> LAST_SWING_TIME = Tag.Long("lastSwingTime");
	public static final Tag<Boolean> BLOCKING_SWORD = Tag.Boolean("blockingSword");
	public static final Tag<ItemStack> BLOCK_REPLACEMENT_ITEM = Tag.ItemStack("blockReplacementItem");
	
	private final ItemStack blockingItem;
	
	public LegacyVanillaBlockFeature(ItemDamageFeature itemDamageFeature, ItemStack blockingItem) {
		super(itemDamageFeature, CombatVersion.LEGACY);
		this.blockingItem = blockingItem;
	}
	
	@CombatSetup
	private static void setup(EventNode<Event> node) {
		node.addListener(AsyncPlayerConfigurationEvent.class, event -> {
			event.getPlayer().setTag(LAST_SWING_TIME, 0L);
			event.getPlayer().setTag(BLOCKING_SWORD, false);
		});
	}
	
	@Override
	public void init(EventNode<EntityInstanceEvent> node) {
		node.addListener(PlayerUseItemEvent.class, this::handleUseItem);
		node.addListener(ItemUpdateStateEvent.class, this::handleUpdateState);
		node.addListener(PlayerSwapItemEvent.class, this::handleSwapItem);
		node.addListener(PlayerChangeHeldSlotEvent.class, this::handleChangeSlot);
		
		node.addListener(PlayerHandAnimationEvent.class, event -> {
			if (event.getHand() == Player.Hand.MAIN)
				event.getPlayer().setTag(LAST_SWING_TIME, System.currentTimeMillis());
		});
	}
	
	@Override
	public boolean isBlocking(Player player) {
		return player.getTag(BLOCKING_SWORD);
	}
	
	@Override
	public void block(Player player) {
		if (!isBlocking(player)) {
			player.setTag(BLOCK_REPLACEMENT_ITEM, player.getItemInOffHand());
			player.setTag(BLOCKING_SWORD, true);
			
			player.setItemInOffHand(blockingItem);
			player.refreshActiveHand(true, true, false);
			player.sendPacketToViewersAndSelf(player.getMetadataPacket());
		}
	}
	
	@Override
	public void unblock(Player player) {
		if (isBlocking(player)) {
			player.setTag(BLOCKING_SWORD, false);
			player.setItemInOffHand(player.getTag(BLOCK_REPLACEMENT_ITEM));
			player.removeTag(BLOCK_REPLACEMENT_ITEM);
		}
	}
	
	private void handleUseItem(PlayerUseItemEvent event) {
		Player player = event.getPlayer();
		
		if (event.getHand() == Player.Hand.MAIN && !isBlocking(player) && canBlockWith(player, event.getItemStack())) {
			long elapsedSwingTime = System.currentTimeMillis() - player.getTag(LAST_SWING_TIME);
			if (elapsedSwingTime < 50) {
				return;
			}
			
			block(player);
		}
	}
	
	protected void handleUpdateState(ItemUpdateStateEvent event) {
		if (event.getHand() == Player.Hand.OFF && event.getItemStack().isSimilar(blockingItem))
			unblock(event.getPlayer());
	}
	
	protected void handleSwapItem(PlayerSwapItemEvent event) {
		Player player = event.getPlayer();
		if (player.getItemInOffHand().isSimilar(blockingItem) && isBlocking(player))
			event.setCancelled(true);
	}
	
	protected void handleChangeSlot(PlayerChangeHeldSlotEvent event) {
		Player player = event.getPlayer();
		if (player.getItemInOffHand().isSimilar(blockingItem) && isBlocking(player))
			unblock(player);
	}
	
	@Override
	public boolean canBlockWith(Player player, ItemStack stack) {
		return stack.material().registry().namespace().value().contains("sword");
	}
}
