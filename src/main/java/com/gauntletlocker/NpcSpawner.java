package com.gauntletlocker

import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.ConfigManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class NpcSpawner
{ 
  private static ClientSideNpc deathNpc = null;
  
  private static void constructNpc(RuneliteObjectManager runeliteObjectManager, CLient client)
  {
    WorldPoint playerPos = client.getLocalPlayer().getWorldLocation();
		WorldPoint posByPLayer = new WorldPoint(playerPos.getX(), playerPos.getY() + 1, playerPos.getPlane());
		ClientSideNpc npc = runeliteObjectManager.createFakeNpc("global", constructNpcObject(client), posByPLayer, 862);
		npc.setName("Death");
		npc.setExamine("Will not let you in!");
		npc.addExamineAction(runeliteObjectManager);
		npc.disable();
    deathNpc = npc;
  }

  private static Model constructNpcObject(CLient client)
  {
    NPCComposition npc = client.getNpcDefinition(NpcID.HALLOWEEN_DEATH);
		int[] models = npc.getModels();
		short[] coloursToReplace = npc.getColorToReplace();
		short[] coloursToReplaceWith = npc.getColorToReplaceWith();
		ModelData mdf = createModel(client, models);

		if (coloursToReplace != null && coloursToReplaceWith != null && coloursToReplace.length == coloursToReplaceWith.length) {
			for (int i=0; i < coloursToReplace.length; i++)
			{
				mdf.recolor(coloursToReplace[i], coloursToReplaceWith[i]);
			}
		}
		return mdf.cloneColors()
			.light();
  }
}
