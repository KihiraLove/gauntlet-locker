package com.gauntletlocker;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("gauntletlocker")
public interface GauntletLockerConfig extends Config
{
	@ConfigSection(
		name = "Overlay",
		description = "Configure the locked Gauntlet Portal overlay",
		position = 0,
		closedByDefault = true
	)
	String overlaySection = "overlay";

	@Alpha
	@ConfigItem(
		keyName = "overlaycolour",
		name = "Overlay colour",
		description = "Fill colour for the locked Gauntlet Portal",
		position = 0,
		section = overlaySection
	)
	default Color overlayColour()
	{
		return new Color(211, 211, 211, 105);
	}

	@ConfigItem(
		keyName = "examinetext",
		name = "Examine text",
		description = "Replacement examine text for the Gauntlet Portal",
		position = 1
	)
	default String examineText()
	{
		return "Does not open from this side.";
	}
}
