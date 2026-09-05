package com.gauntletlocker;

import com.google.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.SpriteID;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

public class GauntletPortalOverlay extends Overlay
{
	private final Client client;
	private final GauntletLockerPlugin plugin;
	private final GauntletLockerConfig config;
	private final SpriteManager spriteManager;

	@Inject
	public GauntletPortalOverlay(
		Client client,
		GauntletLockerPlugin plugin,
		GauntletLockerConfig config,
		SpriteManager spriteManager)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
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
		graphics.setColor(config.overlayColour());

		for (GameObject object : plugin.getPortals())
		{
			if (object == null || object.getPlane() != client.getPlane())
			{
				continue;
			}

			Shape hull = object.getConvexHull();
			if (hull != null)
			{
				graphics.fill(hull);
			}
		}
	}

	private void renderDeathOverhead(Graphics2D graphics)
	{
		RuneLiteObject death = plugin.getDeathObject();
		String text = plugin.getDeathOverheadText();
		if (death == null || text == null || !death.isActive() || death.getBaseModel() == null)
		{
			return;
		}

		int height = death.getBaseModel().getModelHeight() + 40;
		Point location = Perspective.localToCanvas(
			client,
			death.getLocation(),
			death.getLevel(),
			height);

		if (location != null)
		{
			OverlayUtil.renderTextLocation(graphics, location, text, Color.WHITE);
		}
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
