package com.gauntletlocker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("gauntletlocker")
public interface GauntletLockerConfig extends Config
{
	@ConfigItem(
		keyName = "examinetext",
		name = "Examine text",
		description = "Replacement examine text for the Gauntlet Portal",
		position = 0
	)
	default String examineText()
	{
		return "Does not open from this side.";
	}
}
