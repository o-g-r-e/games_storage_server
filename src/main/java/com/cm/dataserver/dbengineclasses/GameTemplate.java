package com.cm.dataserver.dbengineclasses;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import com.cm.dataserver.basedbclasses.Field;
import com.cm.dataserver.basedbclasses.TableIndex;
import com.cm.dataserver.basedbclasses.TableTemplate;


public class GameTemplate {
	private String name;
	private List<TableTemplate> tableTemplates;
	
	public GameTemplate(String name, List<TableTemplate> tableTemplates) {
		this.name = name;
		this.tableTemplates = tableTemplates;
	}

	public List<TableTemplate> getTableTemplates() {
		return tableTemplates;
	}
	
	public String[] getTableNames() {
		List<String> result = new ArrayList<>();
		
		for(TableTemplate tableTemplate : tableTemplates) {
			result.add(tableTemplate.getName());
		}
		
		return result.toArray(new String[] {});
	}

	public String getType() {
		return name;
	}
	
	public static GameTemplate match3Template() {
		
		List<TableTemplate> tblTemplates = new ArrayList<>();
		TableTemplate levelsTemplate = new TableTemplate("levels", new Field[] {  new Field(Types.INTEGER, "id").defNull(false).setAutoIncrement(true),
																				  new Field(Types.VARCHAR, "playerId").defNull(false).setLength(17),
				  																  new Field(Types.INTEGER, "level").defNull(false),
																				  new Field(Types.INTEGER, "score").defNull(false),
																				  new Field(Types.INTEGER, "stars").defNull(false)}, "id");
		
		levelsTemplate.addIndex(new TableIndex("playerId_level", new String[] {"playerId", "level"}, true));
		
		TableTemplate playersTemplate = new TableTemplate("players", new Field[] {  new Field(Types.VARCHAR, "playerId").defNull(false).setLength(17),
																					new Field(Types.VARCHAR, "facebookId").defNull(false)}, "playerId");
		
		playersTemplate.addIndex(new TableIndex("playerId_unique", new String[] {"facebookId"}, true));
				
		TableTemplate boostsTemplate = new TableTemplate("boosts", new Field[] {new Field(Types.INTEGER, "id").defNull(false).setAutoIncrement(true),
																				new Field(Types.VARCHAR, "playerId").defNull(false).setLength(17), 
				  																new Field(Types.VARCHAR, "name").defNull(false).setLength(24), 
																				new Field(Types.INTEGER, "count").setDefaultValue("0")}, "id");
		
		boostsTemplate.addIndex(new TableIndex("playerId_boostName", new String[] {"playerId", "name"}, true));
		
		TableTemplate messagesTemplate = new TableTemplate("messages", new Field[] {new Field(Types.INTEGER, "id").defNull(false).setAutoIncrement(true),
																					new Field(Types.VARCHAR, "type").defNull(false).setLength(15), 
																					new Field(Types.VARCHAR, "sender_id").defNull(false).setLength(17), 
																					new Field(Types.VARCHAR, "recipient_id").defNull(false).setLength(17), 
																					new Field(Types.VARCHAR, "message_content").defNull(false).setLength(24)}, "id");
		
		TableTemplate lifeRequestsTemplate = new TableTemplate("life_requests", new Field[] {new Field(Types.VARCHAR, "id").defNull(false).setLength(32),
																				new Field(Types.VARCHAR, "life_sender").defNull(false).setLength(17), 
																				new Field(Types.VARCHAR, "life_receiver").defNull(false).setLength(17), 
																				new Field(Types.VARCHAR, "status").defNull(false).setLength(9)}, "id");
		
		lifeRequestsTemplate.addIndex(new TableIndex("sender_receiver", new String[] {"life_sender", "life_receiver"}, true));
		
		tblTemplates.add(levelsTemplate);
		tblTemplates.add(playersTemplate);
		tblTemplates.add(boostsTemplate);
		tblTemplates.add(messagesTemplate);
		tblTemplates.add(lifeRequestsTemplate);
		
		return new GameTemplate("Match 3", tblTemplates);
	}
}
