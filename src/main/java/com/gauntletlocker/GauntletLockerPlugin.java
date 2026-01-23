package com.gauntletlocker;

import com.google.inject.Inject;
import java.util.HashSet;
import java.util.Set;

import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Player;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(name = "Gauntlet Locker")
public class GauntletLockerPlugin extends Plugin
{
	@Inject private Client client;
	@Inject private ClientThread clientThread;

	@Inject private OverlayManager overlayManager;
	@Inject private GauntletPortalOverlay overlay;

	@Inject private ChatboxPanelManager chatboxPanelManager;
	@Inject private KeyManager keyManager;
	@Inject private MouseManager mouseManager;
	@Inject private GauntletLockerConfig config;
	@Inject private ConfigManager configManager;

	// Portal
	static final int PORTAL_OBJECT_ID = 36081;
	static final String PORTAL_NAME = "Gauntlet Portal";

	// Sequence

	private static final int SPOTANIM_KEY = 1;


	// Overhead duration in client cycles (20ms)
	private static final int OVERHEAD_CYCLES = 100;

	// Dialogue (your values)
	private static final int REAPER_NPC_ID = 9855;
	private static final int REAPER_CHATHEAD_SEQ = 563;

	private static final String REAPER_NAME = "Grim Reaper";
	private static final String REAPER_DIALOGUE = "you're not welcome here";

	// Menu recolor
	private static final String GREY_OPEN = "<col=808080>";
	private static final String GREY_CLOSE = "</col>";

	private enum Phase { IDLE, WAIT_ANIM, WAIT_DIALOGUE, DIALOGUE_OPEN }


	@Provides
	GauntletLockerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(GauntletLockerConfig.class);
	}

	@Getter
	private final Set<GameObject> portals = new HashSet<>();

	private Phase phase = Phase.IDLE;

	private boolean animSeen = false;
	private int animStartCycle = 0;
	private int dialogueOpenCycle = -1;

	private GrimReaperDialogueInput reaperDialogue;

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
		portals.clear();
		phase = Phase.IDLE;
		dialogueOpenCycle = -1;
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		portals.clear();
		phase = Phase.IDLE;
		dialogueOpenCycle = -1;

		closeReaperDialogue();
	}

	// Track portals for overlay
	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned e)
	{
		GameObject obj = e.getGameObject();
		if (obj != null && obj.getId() == PORTAL_OBJECT_ID)
		{
			portals.add(obj);
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned e)
	{
		GameObject obj = e.getGameObject();
		if (obj != null && obj.getId() == PORTAL_OBJECT_ID)
		{
			portals.remove(obj);
		}
	}

	// Grey the "Enter" menu entry
	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded e)
	{
		if (e.getIdentifier() != PORTAL_OBJECT_ID)
		{
			return;
		}

		final String opt = Text.removeTags(e.getOption());
		final String tgt = Text.removeTags(e.getTarget());

		final boolean isEnter = opt.equalsIgnoreCase("Enter") || opt.equalsIgnoreCase("Enter " + PORTAL_NAME);
		final boolean isPortal = (tgt != null && tgt.toLowerCase().contains(PORTAL_NAME.toLowerCase()))
				|| opt.toLowerCase().contains(PORTAL_NAME.toLowerCase());

		if (!isEnter || !isPortal)
		{
			return;
		}

		final var me = e.getMenuEntry();
		me.setOption(GREY_OPEN + opt + GREY_CLOSE);
		if (tgt != null && !tgt.isEmpty())
		{
			me.setTarget(GREY_OPEN + tgt + GREY_CLOSE);
		}
	}

	// Intercept clicks
	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked e)
	{
		if (e.getId() != PORTAL_OBJECT_ID)
		{
			return;
		}

		final String opt = Text.removeTags(e.getMenuOption());
		final String tgt = Text.removeTags(e.getMenuTarget());

		final boolean isPortal = (tgt != null && tgt.toLowerCase().contains(PORTAL_NAME.toLowerCase()))
				|| opt.toLowerCase().contains(PORTAL_NAME.toLowerCase());

		if (!isPortal)
		{
			return;
		}

		if (opt.equalsIgnoreCase("Enter") || opt.equalsIgnoreCase("Enter " + PORTAL_NAME))
		{
			e.consume();
			startSequence();
			return;
		}

		if (opt.equalsIgnoreCase("Examine"))
		{
			e.consume();
			client.addChatMessage(ChatMessageType.OBJECT_EXAMINE, "", config.examineText(), null);
		}
	}

	private void startSequence()
	{
		closeReaperDialogue();

		phase = Phase.WAIT_ANIM;
		animSeen = false;
		animStartCycle = client.getGameCycle();
		dialogueOpenCycle = -1;

		clientThread.invokeLater(() ->
		{
			Player p = client.getLocalPlayer();
			if (p != null)
			{
				p.setAnimation(config.animationId());
			}
		});
	}

	@Subscribe
	public void onClientTick(ClientTick tick)
	{
		// If chatbox was closed (e.g. click), unregister listeners and clear reference
		if (reaperDialogue != null && reaperDialogue.isClosed())
		{
			unregisterDialogueListeners();
			reaperDialogue = null;
			if (phase == Phase.DIALOGUE_OPEN)
			{
				phase = Phase.IDLE;
			}
		}

		final Player p = client.getLocalPlayer();
		if (p == null)
		{
			return;
		}

		if (phase == Phase.WAIT_ANIM)
		{
			final int anim = p.getAnimation();

			if (anim == config.animationId())
			{
				animSeen = true;
				return;
			}

			if (animSeen)
			{
				afterAnimation();
				return;
			}

			if (client.getGameCycle() - animStartCycle > 60)
			{
				afterAnimation();
			}
			return;
		}

		// Open dialogue ONCE
		if (phase == Phase.WAIT_DIALOGUE && dialogueOpenCycle != -1 && client.getGameCycle() >= dialogueOpenCycle)
		{
			openReaperDialogue();
		}
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		// Close on next tick after SPACE (dialogue sets closeRequested)
		if (reaperDialogue != null && reaperDialogue.isCloseRequested())
		{
			closeReaperDialogue();
			phase = Phase.IDLE;
		}
	}

	private void afterAnimation()
	{
		phase = Phase.WAIT_DIALOGUE;

		clientThread.invokeLater(() ->
		{
			final Player p = client.getLocalPlayer();
			if (p == null)
			{
				phase = Phase.IDLE;
				return;
			}

			p.createSpotAnim(SPOTANIM_KEY, config.spotAnimationId(), config.spotAnimationOffset(), config.spotAnimationDelay());
			client.playSoundEffect(config.soundEffectId());

			p.setOverheadText(config.overheadMessage());
			p.setOverheadCycle(OVERHEAD_CYCLES);
			client.addChatMessage(ChatMessageType.PUBLICCHAT, "", config.chatMessage(), client.getLocalPlayer().getName());

			dialogueOpenCycle = client.getGameCycle() + OVERHEAD_CYCLES;
		});
	}

	private void openReaperDialogue()
	{
		if (reaperDialogue != null) // already open
		{
			return;
		}

		// mark as open so onClientTick doesn't reopen it forever
		phase = Phase.DIALOGUE_OPEN;
		dialogueOpenCycle = -1;

		final int REAPER_NPC_ID = 9855;
		final int CHATHEAD_SEQ = 563;

		reaperDialogue = new GrimReaperDialogueInput(
				clientThread,
				chatboxPanelManager,
				REAPER_NPC_ID,
				CHATHEAD_SEQ,
				"Grim Reaper",
				"you're not welcome here"
		);

		chatboxPanelManager.openInput(reaperDialogue);
	}


	private void closeReaperDialogue()
	{
		if (reaperDialogue != null)
		{
			unregisterDialogueListeners();
			reaperDialogue = null;
		}
		chatboxPanelManager.close();
	}

	private void registerDialogueListeners()
	{
		if (reaperDialogue == null)
		{
			return;
		}
		keyManager.registerKeyListener(reaperDialogue);
		mouseManager.registerMouseListener(reaperDialogue);
	}

	private void unregisterDialogueListeners()
	{
		if (reaperDialogue == null)
		{
			return;
		}
		keyManager.unregisterKeyListener(reaperDialogue);
		mouseManager.unregisterMouseListener(reaperDialogue);
	}
}