package com.gauntletlocker;

import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;

public class ClientSideNpc extends ExtendedRuneliteObject
{
	@Setter
	int idleAnimation;

	Integer secondAnimation;

	@Setter
	boolean alwaysFacePlayer;
	protected ClientSideNpc(Client client, ClientThread clientThread, WorldPoint worldPoint, int[] model, int animation)
	{
		super(client, clientThread, worldPoint, model, animation);
		objectType = RuneliteObjectTypes.NPC;
	}

	protected ClientSideNpc(Client client, ClientThread clientThread, WorldPoint worldPoint, Model model, int animation)
	{
		super(client, clientThread, worldPoint, model, animation);
		this.idleAnimation = animation;
		objectType = RuneliteObjectTypes.NPC;
	}


	protected ClientSideNpc(Client client, ClientThread clientThread, WorldPoint worldPoint, int[] model, int animation, int idleAnimation)
	{
		super(client, clientThread, worldPoint, model, animation);
		objectType = RuneliteObjectTypes.NPC;
		this.idleAnimation = idleAnimation;
	}

	public void setAnimation(int animation, int secondAnimation)
	{
		if (this.animation == animation)
		{
			return;
		}
		this.animation = animation;
		this.secondAnimation = secondAnimation;
		update();
	}

	@Override
	protected void update()
	{
		clientThread.invoke(() ->
		{
			runeliteObject.setAnimation(client.loadAnimation(animation));
			runeliteObject.setModel(model);
			runeliteObject.setShouldLoop(true);

			return true;
		});
	}

	@Override
	protected void actionOnClientTick()
	{
		super.actionOnClientTick();
		if (animation != idleAnimation)
		{
			if (runeliteObject.getAnimation().getNumFrames() <= runeliteObject.getAnimationFrame() + 1)
			{
				if (secondAnimation != null)
				{
					setAnimation(secondAnimation);
					this.secondAnimation = null;
				}
				else
				{
					setAnimation(idleAnimation);
				}
			}
		}
		if (alwaysFacePlayer)
		{
			setOrientationGoalAsPlayer(client);
		}
	}
}
