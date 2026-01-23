package com.gauntletlocker;

import net.runelite.client.config.*;

import java.awt.*;

@ConfigGroup("gauntletlocker")
public interface GauntletLockerConfig extends Config
{
	@ConfigSection(
			name = "Animations and sounds",
			description = "Configure animations and sounds",
			position = 0,
			closedByDefault = true
	)
	String idsSection = "ids";

	@ConfigSection(
			name = "Overlay",
			description = "Configure the overlay of the Gauntlet Portal",
			position = 1,
			closedByDefault = true
	)
	String overlaySection = "overlay";

	@ConfigItem(
			keyName = "animid",
			name = "Animation ID",
			description = "ID for animation to be played",
			position = 0,
			section = idsSection
	)
	default int animationId()
	{
		return 881;
	}

	@ConfigItem(
			keyName = "spotanimid",
			name = "Spot Animation ID",
			description = "ID for spot animation to be played",
			position = 1,
			section = idsSection
	)
	default int spotAnimationId()
	{
		return 245;
	}

	@ConfigItem(
			keyName = "spotanimdelay",
			name = "Spot Animation delay",
			description = "Delay for spot animation",
			position = 2,
			section = idsSection
	)
	default int spotAnimationDelay()
	{
		return 0;
	}

	@ConfigItem(
			keyName = "spotanimoffset",
			name = "Spot Animation offset",
			description = "Height offset for spot animation",
			position = 3,
			section = idsSection
	)
	default int spotAnimationOffset()
	{
		return 80;
	}

	@ConfigItem(
			keyName = "soundeffectid",
			name = "Sound effect ID",
			description = "ID for sound effect",
			position = 4,
			section = idsSection
	)
	default int soundEffectId()
	{
		return 2727;
	}

	@ConfigItem(
			keyName = "overheadmessage",
			name = "Overhead Message",
			description = "Overhead text message to show after animation",
			position = 5,
			section = idsSection
	)
	default String overheadMessage()
	{
		return "It seems to be locked.";
	}

	@ConfigItem(
			keyName = "overheadmessage",
			name = "Chat Message",
			description = "Chat text message to show after animation",
			position = 6,
			section = idsSection
	)
	default String chatMessage()
	{
		return "It seems to be locked.";
	}

	@ConfigItem(
			keyName = "examinetext",
			name = "Examine Text",
			description = "Examine text for the Gauntlet Portal",
			position = 7,
			section = idsSection
	)
	default String examineText()
	{
		return "Does not open from this side.";
	}

	@Alpha
	@ConfigItem(
			keyName = "overlaycolour",
			name = "Overlay Colour",
			description = "Fill colour for Gauntlet Portal overlay",
			position = 1,
			section = overlaySection
	)
	default Color overlayColour()
	{
		return new Color(0, 0, 0, 200);
	}
}
