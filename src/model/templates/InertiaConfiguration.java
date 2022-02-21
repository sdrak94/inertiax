package inertiax.model.templates;

import java.util.HashMap;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import inertiax.enums.EAutoAttack;
import inertiax.enums.EMoveType;
import inertiax.enums.ESearchType;
import l2.ae.pvp.gameserver.model.StatsSet;

public class InertiaConfiguration
{
	private final String									_templateId;
	private final EMoveType									_moveType;
	private final ESearchType								_searchType;
	private final EAutoAttack								_autoAttack;
	private final HashMap<String, InertiaActionTemplate>	_actions	= new HashMap<>();
	
	private int actionCount;
	
	public InertiaConfiguration(Node n)
	{
		final StatsSet set = new StatsSet();
		final NamedNodeMap nnm = n.getAttributes();
		_templateId = nnm.getNamedItem("id").getNodeValue();
		for (Node n1 = n.getFirstChild(); n1 != null; n1 = n1.getNextSibling())
		{
			if ("options".equalsIgnoreCase(n1.getNodeName()))
			{
				for (Node n2 = n1.getFirstChild(); n2 != null; n2 = n2.getNextSibling())
				{
					if ("opt".equalsIgnoreCase(n2.getNodeName()))
					{
						final NamedNodeMap nnm2 = n2.getAttributes();
						set.set(nnm2.getNamedItem("name").getNodeValue(), nnm2.getNamedItem("value").getNodeValue());
					}
				}
			}
			if ("actions".equalsIgnoreCase(n1.getNodeName()))
			{
				for (Node n2 = n1.getFirstChild(); n2 != null; n2 = n2.getNextSibling())
				{
					if ("action".equalsIgnoreCase(n2.getNodeName()))
					{
						final var action = new InertiaActionTemplate(n2, actionCount++);
						_actions.put(action.getAlias(), action);
					}
				}
			}
		}
		_moveType = set.getEnum("MoveType", EMoveType.class, EMoveType.Current_Location);
		_searchType = set.getEnum("SearchType", ESearchType.class, ESearchType.Far);
		_autoAttack = set.getEnum("AutoAttack", EAutoAttack.class, EAutoAttack.Skills_Reuse);
	}
	
	public String getTemplateId()
	{
		return _templateId;
	}
	
	public EMoveType getMoveType()
	{
		return _moveType;
	}
	
	public ESearchType getSearchType()
	{
		return _searchType;
	}
	
	public EAutoAttack getAutoAttack()
	{
		return _autoAttack;
	}
	
	public HashMap<String, InertiaActionTemplate> getActions()
	{
		return _actions;
	}
}
