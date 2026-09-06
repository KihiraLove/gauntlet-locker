package com.gauntletlocker;

import com.google.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.SpriteID;
import net.runelite.api.WorldView;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

public class GauntletPortalOverlay extends Overlay
{
	private static final Color PORTAL_LOCK_COLOUR = new Color(211, 211, 211, 105);

	private final Client client;
	private final GauntletLockerPlugin plugin;
	private final SpriteManager spriteManager;

	@Inject
	public GauntletPortalOverlay(
		Client client,
		GauntletLockerPlugin plugin,
		SpriteManager spriteManager)
	{
		this.client = client;
		this.plugin = plugin;
		this.spriteManager = spriteManager;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		renderPortalLock(graphics);
		renderDeathOverhead(graphics);
		renderRedClick(graphics);
		return null;
	}

	private void renderPortalLock(Graphics2D graphics)
	{
		Player player = client.getLocalPlayer();
		Shape playerHull = player == null ? null : player.getConvexHull();
		Shape deathClickbox = getDeathClickbox();

		graphics.setColor(PORTAL_LOCK_COLOUR);

		for (GameObject object : plugin.getPortals())
		{
			if (object == null || object.getPlane() != client.getPlane())
			{
				continue;
			}

			Shape hull = object.getConvexHull();
			if (hull == null)
			{
				continue;
			}

			Area visiblePortal = new Area(hull);
			if (playerHull != null)
			{
				visiblePortal.subtract(new Area(playerHull));
			}
			if (deathClickbox != null)
			{
				visiblePortal.subtract(new Area(deathClickbox));
			}

			graphics.fill(visiblePortal);
		}
	}

	private void renderDeathOverhead(Graphics2D graphics)
	{
		RuneLiteObject death = plugin.getDeathObject();
		String text = plugin.getDeathOverheadText();
		if (death == null || text == null || !death.isActive())
		{
			return;
		}

		FontMetrics fontMetrics = graphics.getFontMetrics();
		Shape clickbox = getDeathClickbox();
		Point textLocation = null;

		if (clickbox != null)
		{
			Rectangle bounds = clickbox.getBounds();
			int x = bounds.x + (bounds.width - fontMetrics.stringWidth(text)) / 2;
			int y = bounds.y - 8;
			textLocation = new Point(x, y);
		}
		else
		{
			Point projected = Perspective.localToCanvas(
				client,
				death.getLocation(),
				death.getLevel(),
				300);
			if (projected != null)
			{
				textLocation = new Point(
					projected.getX() - fontMetrics.stringWidth(text) / 2,
					projected.getY());
			}
		}

		if (textLocation != null)
		{
			OverlayUtil.renderTextLocation(graphics, textLocation, text, Color.WHITE);
		}
	}

	private Shape getDeathClickbox()
	{
		RuneLiteObject death = plugin.getDeathObject();
		if (death == null || !death.isActive() || death.getModel() == null)
		{
			return null;
		}

		WorldView worldView = client.getWorldView(death.getWorldView());
		if (worldView == null)
		{
			return null;
		}

		return Perspective.getClickbox(
			client,
			worldView,
			death.getModel(),
			death.getOrientation(),
			death.getLocation().getX(),
			death.getLocation().getY(),
			death.getZ());
	}

	private void renderRedClick(Graphics2D graphics)
	{
		Point click = plugin.getRedClickPoint();
		int startCycle = plugin.getRedClickStartCycle();
		if (click == null || startCycle < 0)
		{
			return;
		}

		int elapsed = client.getGameCycle() - startCycle;
		if (elapsed < 0 || elapsed >= GauntletLockerPlugin.RED_CLICK_CYCLES)
		{
			return;
		}

		int frame = Math.min(3, elapsed / 5);
		BufferedImage sprite = spriteManager.getSprite(SpriteID.RED_CLICK_ANIMATION_1 + frame, 0);
		if (sprite == null)
		{
			return;
		}

		graphics.drawImage(
			sprite,
			click.getX() - sprite.getWidth() / 2,
			click.getY() - sprite.getHeight() / 2,
			null);
	}
}
