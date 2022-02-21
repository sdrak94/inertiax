package inertiax.util;

import java.io.File;
import java.io.FileWriter;

import l2.ae.pvp.gameserver.datatables.NpcTable;

public class SpawnUtil
{
	public void dumpSpawn(int npcId, int x, int y, int z, int heading, int respawnDelay)
	{
		try
		{
			File f = new File("data/xml/spawns/Util_spawndump.xml");
			if (!f.exists())
				f.createNewFile();
			FileWriter writer = new FileWriter(f, true);
			writer.write("\t\t<npc id=\"" + npcId + "\" x=\""+ x + "\" y=\""+ y +"\" z=\""+ z + "\" heading=\""+ heading + "\" respawnDelay=\"" + respawnDelay + "\" />" + " <!--" + NpcTable.getInstance().getTemplate(npcId).getName() + "-->\n");
			writer.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	public boolean isMonster(String npcType)
	{
		switch(npcType)
		{
			case "L2Apc" :
			case "L2Monster":
			case "L2RaidBoss":
			case "L2FestivalMonster":
			case "L2SepulcherMonster":
			case "L2Chest":
			case "L2RiftInvader":
				return true;
		}
		return false;
		
	}
	
	public static class InstanceHolder
	{
		private static final SpawnUtil _instance = new SpawnUtil();
	}
	
	public static final SpawnUtil getInstance()
	{
		return InstanceHolder._instance;
	}
}
