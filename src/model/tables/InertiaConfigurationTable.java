package inertiax.model.tables;

import java.io.File;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import inertiax.model.templates.InertiaConfiguration;

public class InertiaConfigurationTable
{
	private final HashMap<String, InertiaConfiguration> inertiaTemplates = new HashMap<>();

	private InertiaConfigurationTable()
	{
		try
		{
			load();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private void load() throws Exception
	{
		final DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newDefaultInstance();
		final DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
		
		final File templatesFolder = new File("./data/xml/inertia/configurations/");
		
		for (final File templateFile : templatesFolder.listFiles())
		{
			final Document doc = docBuilder.parse(templateFile);
			
			for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if ("configurations".equalsIgnoreCase(n.getNodeName()))
				{
					for (Node n1 = n.getFirstChild(); n1 != null; n1 = n1.getNextSibling())
					{
						if ("configuration".equalsIgnoreCase(n1.getNodeName()))
						{
							final InertiaConfiguration inertiaExtTemplate = new InertiaConfiguration(n1);
							
							final String templateId = inertiaExtTemplate.getTemplateId();
							
							inertiaTemplates.put(templateId, inertiaExtTemplate);
						}
					}
				}
				
				
			}
			
		}
		System.out.println("InertiaConfigurationTable Loaded " + inertiaTemplates.size() + " templates.");
	}
	
	public void reload()
	{
		try
		{
			inertiaTemplates.clear();
			load();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public InertiaConfiguration getById(final String templateId)
	{
		return inertiaTemplates.get(templateId);
	}

	public static class InstanceHolder
	{
		private static final InertiaConfigurationTable _instance = new InertiaConfigurationTable();
	}

	public static InertiaConfigurationTable getInstance()
	{
		return InstanceHolder._instance;
	}
}
