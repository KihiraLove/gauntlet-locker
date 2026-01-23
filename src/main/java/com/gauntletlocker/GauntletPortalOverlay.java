package com.gauntletlocker;

import com.google.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class GauntletPortalOverlay extends Overlay
{

    private final Client client;
    private final GauntletLockerPlugin plugin;
    @Inject private GauntletLockerConfig config;


    @Inject
    public GauntletPortalOverlay(Client client, GauntletLockerPlugin plugin)
    {
        this.client = client;
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D g)
    {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(config.overlayColour());

        for (GameObject obj : plugin.getPortals())
        {
            if (obj == null || obj.getPlane() != client.getPlane())
            {
                continue;
            }

            Shape hull = obj.getConvexHull();
            if (hull != null)
            {
                g.fill(hull);
            }
        }

        return null;
    }
}