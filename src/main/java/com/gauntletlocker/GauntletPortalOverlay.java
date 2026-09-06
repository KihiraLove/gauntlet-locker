package com.gauntletlocker;

import com.google.inject.Inject;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Model;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.SpriteID;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

public class GauntletPortalOverlay extends Overlay
{
	private static final Color PORTAL_LOCK_COLOUR = new Color(211, 211, 211, 105);
	private static final BasicStroke SILHOUETTE_EDGE_STROKE = new BasicStroke(2.0f);

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

		Player player = client.getLocalPlayer();
		Shape playerSilhouette = buildActorSilhouette(player);
		Shape deathSilhouette = buildDeathSilhouette();

		renderPortalLock(graphics, playerSilhouette, deathSilhouette);
		renderDeathOverhead(graphics, deathSilhouette);
		renderRedClick(graphics);
		return null;
	}

	private void renderPortalLock(
		Graphics2D graphics,
		Shape playerSilhouette,
		Shape deathSilhouette)
	{
		graphics.setColor(PORTAL_LOCK_COLOUR);

		for (GameObject object : plugin.getPortals())
		{
			if (object == null || object.getPlane() != client.getPlane())
			{
				continue;
			}

			Shape portalHull = object.getConvexHull();
			if (portalHull == null)
			{
				continue;
			}

			Area visiblePortal = new Area(portalHull);
			if (playerSilhouette != null)
			{
				visiblePortal.subtract(new Area(playerSilhouette));
			}
			if (deathSilhouette != null)
			{
				visiblePortal.subtract(new Area(deathSilhouette));
			}

			graphics.fill(visiblePortal);
		}
	}

	private void renderDeathOverhead(Graphics2D graphics, Shape deathSilhouette)
	{
		RuneLiteObject death = plugin.getDeathObject();
		String text = plugin.getDeathOverheadText();
		if (death == null || text == null || !death.isActive())
		{
			return;
		}

		Font originalFont = graphics.getFont();
		graphics.setFont(FontManager.getRunescapeBoldFont());

		try
		{
			FontMetrics fontMetrics = graphics.getFontMetrics();
			Point textLocation = null;

			if (deathSilhouette != null)
			{
				Rectangle bounds = deathSilhouette.getBounds();
				int x = bounds.x + (bounds.width - fontMetrics.stringWidth(text)) / 2;
				int y = bounds.y - 5;
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
				OverlayUtil.renderTextLocation(graphics, textLocation, text, Color.YELLOW);
			}
		}
		finally
		{
			graphics.setFont(originalFont);
		}
	}

	private Shape buildActorSilhouette(Actor actor)
	{
		if (actor == null)
		{
			return null;
		}

		Model model = actor.getModel();
		LocalPoint location = actor.getLocalLocation();
		WorldView worldView = actor.getWorldView();
		if (model == null || location == null || worldView == null)
		{
			return null;
		}

		int z = Perspective.getFootprintTileHeight(
			client,
			location,
			worldView.getPlane(),
			actor.getFootprintSize()) - actor.getAnimationHeightOffset();

		return buildModelSilhouette(
			worldView,
			model,
			location.getX(),
			location.getY(),
			z,
			actor.getCurrentOrientation());
	}

	private Shape buildDeathSilhouette()
	{
		RuneLiteObject death = plugin.getDeathObject();
		if (death == null || !death.isActive())
		{
			return null;
		}

		Model model = death.getModel();
		LocalPoint location = death.getLocation();
		WorldView worldView = client.getWorldView(death.getWorldView());
		if (model == null || location == null || worldView == null)
		{
			return null;
		}

		return buildModelSilhouette(
			worldView,
			model,
			location.getX(),
			location.getY(),
			death.getZ(),
			death.getOrientation());
	}

	private Shape buildModelSilhouette(
		WorldView worldView,
		Model model,
		int localX,
		int localY,
		int localZ,
		int orientation)
	{
		int vertexCount = model.getVerticesCount();
		if (vertexCount <= 0)
		{
			return null;
		}

		int[] projectedX = new int[vertexCount];
		int[] projectedY = new int[vertexCount];

		Perspective.modelToCanvas(
			client,
			worldView,
			vertexCount,
			localX,
			localY,
			localZ,
			orientation,
			model.getVerticesX(),
			model.getVerticesZ(),
			model.getVerticesY(),
			projectedX,
			projectedY);

		int[] indices1 = model.getFaceIndices1();
		int[] indices2 = model.getFaceIndices2();
		int[] indices3 = model.getFaceIndices3();
		byte[] transparencies = model.getFaceTransparencies();
		Area silhouette = new Area();

		for (int i = 0; i < model.getFaceCount(); i++)
		{
			if (transparencies != null && (transparencies[i] & 255) >= 254)
			{
				continue;
			}

			int index1 = indices1[i];
			int index2 = indices2[i];
			int index3 = indices3[i];

			if (projectedY[index1] == Integer.MIN_VALUE
				|| projectedY[index2] == Integer.MIN_VALUE
				|| projectedY[index3] == Integer.MIN_VALUE)
			{
				continue;
			}

			int x1 = projectedX[index1];
			int y1 = projectedY[index1];
			int x2 = projectedX[index2];
			int y2 = projectedY[index2];
			int x3 = projectedX[index3];
			int y3 = projectedY[index3];

			if (isBackFace(x1, y1, x2, y2, x3, y3))
			{
				continue;
			}

			Polygon triangle = new Polygon(
				new int[]{x1, x2, x3},
				new int[]{y1, y2, y3},
				3);
			silhouette.add(new Area(triangle));
		}

		if (silhouette.isEmpty())
		{
			return null;
		}

		// Cover antialiased edge pixels without reverting to the oversized convex-hull cutout.
		Area expanded = new Area(silhouette);
		expanded.add(new Area(SILHOUETTE_EDGE_STROKE.createStrokedShape(silhouette)));
		return expanded;
	}

	private static boolean isBackFace(int x1, int y1, int x2, int y2, int x3, int y3)
	{
		return (y2 - y1) * (x3 - x2) - (x2 - x1) * (y3 - y2) <= 0;
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
