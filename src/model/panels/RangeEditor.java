package inertiax.model.panels;

import inertiax.model.Inertia;
import inertiax.model.InertiaPanel;
import l2.ae.pvp.gameserver.model.actor.instance.L2PcInstance;
import l2.ae.pvp.gameserver.network.serverpackets.NpcHtmlMessage;
	
public class RangeEditor extends InertiaPanel
{
	public RangeEditor(final Inertia inertia)
	{
		super(inertia);
	}

	@Override
	public void render(final L2PcInstance viewer)
	{
		
		final var player = _inertia.getActivePlayer();
		if (player == null)
			return;
		
		final var npcHtml = new NpcHtmlMessage();
		npcHtml.setFile("data/html/aepvp/inertiax/rangeeditor.htm");

		
		npcHtml.replace("%id%", _ownerId);
		
		player.sendPacket(npcHtml);
	}
	
}
