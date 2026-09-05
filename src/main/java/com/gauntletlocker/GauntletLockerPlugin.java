package com.gauntletlocker;

import com.google.inject.Inject;
import com.google.inject.Provides;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.AnimationController;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(name = "Gauntlet Locker")
public class GauntletLockerPlugin extends Plugin
{
	static final int PORTAL_OBJECT_ID = 36081;
	static final String PORTAL_NAME = "Gauntlet Portal";

	static final int HALLOWEEN_DEATH_NPC_ID = 5567;
	static final int DEATH_ATTACK_ANIMATION = 440;
	static final int PLAYER_STUN_ANIMATION = 881;
	static final int STUN_SOUND_EFFECT = 2727;
	static final String DEATH_OVERHEAD_TEXT = "I told you this was off limits.";

	static final int RED_CLICK_CYCLES = 20;

	private static final String GREY_OPEN = "<col=808080>";
	private static final String GREY_CLOSE = "</col>";

	@Inject private Client client;
	@Inject private ClientThread clientThread;
	@Inject private OverlayManager overlayManager;
	@Inject private GauntletPortalOverlay overlay;
	@Inject private GauntletLockerConfig config;

	@Getter
	private final Set<GameObject> portals = new HashSet<>();

	@Getter
	private RuneLiteObject deathObject;

	@Getter
	private String deathOverheadText;

	@Getter
	private Point redClickPoint;

	@Getter
	private int redClickStartCycle = -1;

	@Provides
	GauntletLockerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(GauntletLockerConfig.class);
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
		resetSequence();
		clientThread.invoke(this::refreshPortals);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		portals.clear();
		resetSequence();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			refreshPortals();
		}
		else if (event.getGameState() == GameState.LOGIN_SCREEN
			|| event.getGameState() == GameState.HOPPING)
		{
			portals.clear();
			resetSequence();
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		GameObject object = event.getGameObject();
		if (object != null && object.getId() == PORTAL_OBJECT_ID)
		{
			portals.add(object);
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		GameObject object = event.getGameObject();
		if (object != null && object.getId() == PORTAL_OBJECT_ID)
		{
			portals.remove(object);
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (event.getIdentifier() != PORTAL_OBJECT_ID)
		{
			return;
		}

		String option = Text.removeTags(event.getOption());
		String target = Text.removeTags(event.getTarget());

		if (!isEnterOption(option) || !isPortal(option, target))
		{
			return;
		}

		event.getMenuEntry().setOption(GREY_OPEN + option + GREY_CLOSE);
		if (target != null && !target.isEmpty())
		{
			event.getMenuEntry().setTarget(GREY_OPEN + target + GREY_CLOSE);
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (event.getId() != PORTAL_OBJECT_ID)
		{
			return;
		}

		String option = Text.removeTags(event.getMenuOption());
		String target = Text.removeTags(event.getMenuTarget());
		if (!isPortal(option, target))
		{
			return;
		}

		if (isEnterOption(option))
		{
			event.consume();
			recordRedClick();
			startDeathSequence();
			return;
		}

		if (option.equalsIgnoreCase("Examine"))
		{
			event.consume();
			client.addChatMessage(ChatMessageType.OBJECT_EXAMINE, "", config.examineText(), null);
		}
	}

	@Subscribe
	public void onClientTick(ClientTick event)
	{
		if (redClickPoint != null
			&& client.getGameCycle() - redClickStartCycle >= RED_CLICK_CYCLES)
		{
			redClickPoint = null;
			redClickStartCycle = -1;
		}
	}

	private void startDeathSequence()
	{
		clientThread.invoke(() ->
		{
			removeDeathObject();

			Player player = client.getLocalPlayer();
			if (player == null)
			{
				return;
			}

			LocalPoint spawnPoint = getDeathSpawnPoint(player);
			Model deathModel = buildDeathModel();
			if (spawnPoint == null || deathModel == null)
			{
				log.warn("Unable to construct Death sequence");
				return;
			}

			RuneLiteObject death = client.createRuneLiteObject();
			death.setModel(deathModel);
			death.setLocation(spawnPoint, player.getWorldLocation().getPlane());
			death.setOrientation(orientationToward(spawnPoint, player.getLocalLocation()));

			AnimationController attack = new AnimationController(client, DEATH_ATTACK_ANIMATION)
				.setOnFinished(controller ->
				{
					if (deathObject == death)
					{
						removeDeathObject();
					}
				});
			death.setAnimationController(attack);

			deathObject = death;
			deathOverheadText = DEATH_OVERHEAD_TEXT;
			death.setActive(true);

			player.setAnimation(PLAYER_STUN_ANIMATION);
			client.playSoundEffect(STUN_SOUND_EFFECT);
		});
	}

	private void recordRedClick()
	{
		Point mouse = client.getMouseCanvasPosition();
		if (mouse == null)
		{
			return;
		}

		redClickPoint = new Point(mouse.getX(), mouse.getY());
		redClickStartCycle = client.getGameCycle();
	}

	private LocalPoint getDeathSpawnPoint(Player player)
	{
		WorldPoint playerWorld = player.getWorldLocation();
		GameObject nearestPortal = null;
		int nearestDistance = Integer.MAX_VALUE;

		for (GameObject portal : portals)
		{
			if (portal == null || portal.getPlane() != playerWorld.getPlane())
			{
				continue;
			}

			WorldPoint portalWorld = portal.getWorldLocation();
			int distance = portalWorld.distanceTo2D(playerWorld);
			if (distance < nearestDistance)
			{
				nearestDistance = distance;
				nearestPortal = portal;
			}
		}

		if (nearestPortal == null)
		{
			WorldPoint fallback = new WorldPoint(
				playerWorld.getX(),
				playerWorld.getY() + 1,
				playerWorld.getPlane());
			return LocalPoint.fromWorld(client, fallback);
		}

		WorldPoint portalWorld = nearestPortal.getWorldLocation();
		int dx = Integer.compare(playerWorld.getX(), portalWorld.getX());
		int dy = Integer.compare(playerWorld.getY(), portalWorld.getY());
		if (dx == 0 && dy == 0)
		{
			dy = 1;
		}

		WorldPoint spawnWorld = new WorldPoint(
			portalWorld.getX() + dx,
			portalWorld.getY() + dy,
			portalWorld.getPlane());

		LocalPoint local = LocalPoint.fromWorld(client, spawnWorld);
		return local != null ? local : nearestPortal.getLocalLocation();
	}

	private Model buildDeathModel()
	{
		NPCComposition composition = client.getNpcDefinition(HALLOWEEN_DEATH_NPC_ID);
		if (composition == null || composition.getModels() == null)
		{
			return null;
		}

		int[] modelIds = composition.getModels();
		ModelData[] parts = new ModelData[modelIds.length];
		for (int i = 0; i < modelIds.length; i++)
		{
			parts[i] = client.loadModelData(modelIds[i]);
			if (parts[i] == null)
			{
				return null;
			}
		}

		ModelData modelData = client.mergeModels(parts);
		if (modelData == null)
		{
			return null;
		}

		short[] colorsToReplace = composition.getColorToReplace();
		short[] replacementColors = composition.getColorToReplaceWith();
		if (colorsToReplace != null
			&& replacementColors != null
			&& colorsToReplace.length == replacementColors.length)
		{
			modelData = modelData.cloneColors();
			for (int i = 0; i < colorsToReplace.length; i++)
			{
				modelData.recolor(colorsToReplace[i], replacementColors[i]);
			}
		}

		return modelData.light();
	}

	private int orientationToward(LocalPoint from, LocalPoint to)
	{
		int dx = to.getX() - from.getX();
		int dy = to.getY() - from.getY();
		double radians = Math.atan2(-dx, -dy);
		return ((int) Math.round(radians * 1024.0 / Math.PI)) & 2047;
	}

	private void refreshPortals()
	{
		portals.clear();

		Scene scene = client.getScene();
		if (scene == null || scene.getTiles() == null)
		{
			return;
		}

		Tile[][][] tiles = scene.getTiles();
		for (Tile[][] plane : tiles)
		{
			if (plane == null)
			{
				continue;
			}

			for (Tile[] row : plane)
			{
				if (row == null)
				{
					continue;
				}

				for (Tile tile : row)
				{
					if (tile == null)
					{
						continue;
					}

					for (GameObject object : tile.getGameObjects())
					{
						if (object != null && object.getId() == PORTAL_OBJECT_ID)
						{
							portals.add(object);
						}
					}
				}
			}
		}
	}

	private void resetSequence()
	{
		removeDeathObject();
		redClickPoint = null;
		redClickStartCycle = -1;
	}

	private void removeDeathObject()
	{
		if (deathObject != null && deathObject.isActive())
		{
			deathObject.setActive(false);
		}
		deathObject = null;
		deathOverheadText = null;
	}

	private static boolean isEnterOption(String option)
	{
		return option != null
			&& (option.equalsIgnoreCase("Enter")
			|| option.equalsIgnoreCase("Enter " + PORTAL_NAME));
	}

	private static boolean isPortal(String option, String target)
	{
		String portalName = PORTAL_NAME.toLowerCase();
		return (target != null && target.toLowerCase().contains(portalName))
			|| (option != null && option.toLowerCase().contains(portalName));
	}
}
