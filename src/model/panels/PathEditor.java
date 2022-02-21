package inertiax.model.panels;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import inertiax.enums.IInertiaCmd.EInertiaAct;
import inertiax.model.Inertia;
import inertiax.model.InertiaAct;
import inertiax.model.InertiaPanel;
import l2.ae.pvp.gameserver.GameTimeController;
import l2.ae.pvp.gameserver.GeoData;
import l2.ae.pvp.gameserver.model.ILocational;
import l2.ae.pvp.gameserver.model.Location;
import l2.ae.pvp.gameserver.model.actor.L2Character.MoveData;
import l2.ae.pvp.gameserver.model.actor.instance.L2PcInstance;
import l2.ae.pvp.gameserver.model.actor.instance.L2PcInstance.IMoveListener;
import l2.ae.pvp.gameserver.network.serverpackets.ExServerPrimitive;
import l2.ae.pvp.gameserver.network.serverpackets.NpcHtmlMessage;
import l2.ae.pvp.gameserver.pathfinding.AbstractNodeLoc;
import l2.ae.pvp.gameserver.util.Util;
	
public class PathEditor extends InertiaPanel implements IMoveListener
{

	public PathEditor(final Inertia inertia)
	{
		super(inertia);
	}

	@Override
	public void render(final L2PcInstance viewer)
	{
		
		
		
	}
	
	@Override
	protected boolean onBypass(L2PcInstance actor, String cmd, StringTokenizer st)
	{
		if (cmd.equals("create"))
		{
			if (_inertiaPaths.size() > 9)
			{
				actor.sendMessage("You have too many paths!");
				return false;
			}
			
			if (st.hasMoreTokens())
			{
				final String pathName = st.nextToken().toUpperCase();
				
				if (pathName.length() > 20)
				{
					actor.sendMessage("This path name is bigger than 20 characters!");
					return false;
				}
				
				if (_inertiaPaths.containsKey(pathName))
				{
					actor.sendMessage("This path name already exists!");
					return false;
				}
				
				final InertiaPath inertiaPath = new InertiaPath(pathName, actor);
				_inertiaPaths.put(pathName, inertiaPath);
				
				inertiaPath.renderPath(actor);
				
				render(actor);
			}
		}
		else if (cmd.equals("resetpaths"))
		{
			_inertiaPaths.clear();
			render(actor);
		}
		else if (cmd.equals("hidepaths"))
		{
			actor.sendMessage("Hiding all Paths.");
			
			for (final InertiaPath path : _inertiaPaths.values())
				path.hide(actor);
			
			render(actor);
		}
		else if (cmd.equals("rempath"))
		{
			if (st.hasMoreTokens())
			{
				final String pathName = st.nextToken();
				final InertiaPath remPath = _inertiaPaths.remove(pathName);
				if (remPath != null)
				{
					remPath.delete(actor);
					render(actor);
				}
			}
		}
		else if (cmd.equals("togpath"))
		{
			if (st.hasMoreTokens())
			{
				final String pathName = st.nextToken();
				final InertiaPath inertiaPath = _inertiaPaths.get(pathName);
				if (inertiaPath != null)
				{
					inertiaPath.toggleRender();
					inertiaPath.renderPath(actor);
				}
				
				render(actor);
			}
		}
		else if (cmd.equals("openpath"))
		{
			if (st.hasMoreTokens())
			{
				final String pathName = st.nextToken();
				final InertiaPath inertiaPath = _inertiaPaths.get(pathName);
				if (inertiaPath != null)
				{
					inertiaPath.renderPage(actor);
					_selePath = inertiaPath;
				}
			}
		}
		else if (cmd.equals("path"))
		{
			if (st.hasMoreTokens())
			{
				final String pathName = st.nextToken();
				final InertiaPath inertiaPath = _inertiaPaths.get(pathName);
				if (inertiaPath != null)
				{
					final String cmd2 = st.nextToken();
					return inertiaPath.onBypass(actor, cmd2, st);
				}
			}
		}
		else if (cmd.equals("sim"))
		{
			if (st.hasMoreTokens())
			{
				final String pathName = st.nextToken();
				final InertiaPath inertiaPath = _inertiaPaths.get(pathName);
				if (inertiaPath != null)
				{
					inertiaPath.sim(actor);
				}
			}
		}
		

		
		
		return super.onBypass(actor, cmd, st);
	}
	

	
}
