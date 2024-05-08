package io.github.togar2.pvp.feature.food;

import io.github.togar2.pvp.entity.PvpPlayer;
import io.github.togar2.pvp.events.PlayerExhaustEvent;
import io.github.togar2.pvp.feature.provider.ProviderForEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.player.PlayerTickEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.tag.Tag;
import net.minestom.server.world.Difficulty;

import java.util.Objects;

public class ExhaustionFeatureImpl implements ExhaustionFeature {
	public static final Tag<Float> EXHAUSTION = Tag.Float("exhaustion");
	
	private final ProviderForEntity<Difficulty> difficultyFeature;
	private final boolean legacy;
	
	public ExhaustionFeatureImpl(ProviderForEntity<Difficulty> difficultyFeature, boolean legacy) {
		this.difficultyFeature = difficultyFeature;
		this.legacy = legacy;
	}
	
	@Override
	public void init(EventNode<Event> node) {
		node.addListener(AsyncPlayerConfigurationEvent.class, event ->
				event.getPlayer().setTag(EXHAUSTION, 0.0f));
		
		node.addListener(PlayerTickEvent.class, event -> onTick(event.getPlayer()));
		
		node.addListener(PlayerBlockBreakEvent.class, event ->
				addExhaustion(event.getPlayer(), legacy ? 0.025f : 0.005f));
		
		node.addListener(PlayerMoveEvent.class, this::onMove);
	}
	
	protected void onTick(Player player) {
		if (!player.getGameMode().canTakeDamage()) return;
		
		float exhaustion = player.getTag(EXHAUSTION);
		if (exhaustion > 4) {
			player.setTag(EXHAUSTION, exhaustion - 4);
			if (player.getFoodSaturation() > 0) {
				player.setFoodSaturation(Math.max(player.getFoodSaturation() - 1, 0));
			} else if (difficultyFeature.getValue(player) != Difficulty.PEACEFUL) {
				player.setFood(Math.max(player.getFood() - 1, 0));
			}
		}
	}
	
	protected void onMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		
		double xDiff = event.getNewPosition().x() - player.getPosition().x();
		double yDiff = event.getNewPosition().y() - player.getPosition().y();
		double zDiff = event.getNewPosition().z() - player.getPosition().z();
		
		// Check if movement was a jump
		if (yDiff > 0.0D && player.isOnGround()) {
			if (player.isSprinting()) {
				addExhaustion(player, legacy ? 0.8f : 0.2f);
			} else {
				addExhaustion(player, legacy ? 0.2f : 0.05f);
			}
			
			//TODO this should DEFINITELY not be here... but where should it be?
			if (player instanceof PvpPlayer custom)
				custom.jump(); // Velocity change
		}
		
		if (player.isOnGround()) {
			int l = (int) Math.round(Math.sqrt(xDiff * xDiff + zDiff * zDiff) * 100.0f);
			if (l > 0) addExhaustion(player, (player.isSprinting() ? 0.1f : 0.0f) * (float) l * 0.01f);
		} else {
			if (Objects.requireNonNull(player.getInstance()).getBlock(player.getPosition()) == Block.WATER) {
				int l = (int) Math.round(Math.sqrt(xDiff * xDiff + yDiff * yDiff + zDiff * zDiff) * 100.0f);
				if (l > 0) addExhaustion(player, 0.01f * (float) l * 0.01f);
			}
		}
	}
	
	@Override
	public void addExhaustion(Player player, float exhaustion) {
		if (!player.getGameMode().canTakeDamage()) return;
		PlayerExhaustEvent playerExhaustEvent = new PlayerExhaustEvent(player, exhaustion);
		EventDispatcher.callCancellable(playerExhaustEvent, () -> player.setTag(EXHAUSTION,
				Math.min(player.getTag(EXHAUSTION) + playerExhaustEvent.getAmount(), 40)));
	}
	
	@Override
	public void addAttackExhaustion(Player player) {
		addExhaustion(player, legacy ? 0.3f: 0.1f);
	}
	
	@Override
	public void addDamageExhaustion(Player player, DamageType type) {
		addExhaustion(player, (float) type.exhaustion() * (legacy ? 3 : 1));
	}
}
