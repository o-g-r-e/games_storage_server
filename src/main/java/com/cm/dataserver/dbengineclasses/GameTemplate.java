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

		Field[] levelsFields = new Field[] { new Field(Types.INTEGER, "id")      .defNull(false).setAutoIncrement(true),
											 new Field(Types.VARCHAR, "playerId").defNull(false).setLength(17),
				  							 new Field(Types.INTEGER, "level")   .defNull(false),
											 new Field(Types.INTEGER, "score")   .defNull(false),
											 new Field(Types.INTEGER, "stars")   .defNull(false) };

		TableTemplate levelsTemplate = new TableTemplate("levels", levelsFields, "id");
		
		levelsTemplate.addIndex(new TableIndex("playerId_level", new String[] {"playerId", "level"}, true));

		Field[] playersFields = new Field[] { new Field(Types.VARCHAR, "playerId")  .defNull(false).setLength(17),
											  new Field(Types.VARCHAR, "facebookId").defNull(false) };
		
		TableTemplate playersTemplate = new TableTemplate("players", playersFields, "playerId");
		
		playersTemplate.addIndex(new TableIndex("playerId_unique", new String[] {"facebookId"}, true));

		Field[] boostsFields = new Field[] { new Field(Types.INTEGER, "id")      .defNull(false).setAutoIncrement(true),
											 new Field(Types.VARCHAR, "playerId").defNull(false).setLength(17), 
				  							 new Field(Types.VARCHAR, "name")    .defNull(false).setLength(24), 
											 new Field(Types.INTEGER, "count")   .setDefaultValue("0") };
				
		TableTemplate boostsTemplate = new TableTemplate("boosts", boostsFields, "id");
		
		boostsTemplate.addIndex(new TableIndex("playerId_boostName", new String[] {"playerId", "name"}, true));
		
		/* Field[] messagesFileds = new Field[] { new Field(Types.INTEGER, "id").defNull(false).setAutoIncrement(true),
											   new Field(Types.VARCHAR, "type").defNull(false).setLength(15), 
											   new Field(Types.VARCHAR, "sender_id").defNull(false).setLength(17), 
											   new Field(Types.VARCHAR, "recipient_id").defNull(false).setLength(17), 
											   new Field(Types.VARCHAR, "message_content").defNull(false).setLength(24) };

		TableTemplate messagesTemplate = new TableTemplate("messages", messagesFileds, "id"); */
		
		Field[] lifeReuestsFields = new Field[] { new Field(Types.INTEGER, "id")           .defNull(false).setLength(32),
												  new Field(Types.VARCHAR, "life_sender")  .defNull(false).setLength(17), 
												  new Field(Types.VARCHAR, "life_receiver").defNull(false).setLength(17), 
												  new Field(Types.VARCHAR, "status")       .defNull(false).setLength(9) };

		TableTemplate lifeRequestsTemplate = new TableTemplate("life_requests", lifeReuestsFields, "id");
		
		lifeRequestsTemplate.addIndex(new TableIndex("sender_receiver", new String[] {"life_sender", "life_receiver"}, true));
		
		Field[] requestsStatisticFields = new Field[] { new Field(Types.VARCHAR, "uri").defNull(false).setLength(32), 
														new Field(Types.INTEGER, "count") };

		TableTemplate requestsStatisticTemplate = new TableTemplate("requests_statistic", requestsStatisticFields, "uri");
		
		requestsStatisticTemplate.addIndex(new TableIndex("uniq_uri", new String[] {"uri"}, true));
		
		tblTemplates.add(levelsTemplate);
		tblTemplates.add(playersTemplate);
		tblTemplates.add(boostsTemplate);
		//tblTemplates.add(messagesTemplate);
		tblTemplates.add(lifeRequestsTemplate);
		tblTemplates.add(requestsStatisticTemplate);
		
		return new GameTemplate("Match 3", tblTemplates);
	}

	public static GameTemplate casualGameTemplate() {
		List<TableTemplate> tblTemplates = GameTemplate.match3Template().getTableTemplates();

		Field[] eventsFields = new Field[] { new Field(Types.INTEGER, "id").defNull(false).setLength(4),
											 new Field(Types.VARCHAR, "status").defNull(false),
											 new Field(Types.TIMESTAMP, "end").defNull(false),
											 new Field(Types.VARCHAR, "nagrada").defNull(false),
											 new Field(Types.VARCHAR, "winner_id").setLength(17) };

		TableTemplate scoreEventsTemplate = new TableTemplate("score_events", eventsFields, "id");
		
		Field[] initScoreBalanceFields = new Field[] {new Field(Types.VARCHAR, "playerId").defNull(false).setLength(17),
													  new Field(Types.INTEGER, "init_score_balance").defNull(false)};

		TableTemplate initScoreBalanceTemplate = new TableTemplate("initial_score_balance", initScoreBalanceFields, "playerId");

		Field[] lastEventLeadersFields = new Field[] {new Field(Types.VARCHAR, "playerId").defNull(false).setLength(17),
													  new Field(Types.INTEGER, "earned_scores").defNull(false)};

		TableTemplate lastEventLeadersTemplate = new TableTemplate("last_event_leaders", lastEventLeadersFields, "playerId");

		tblTemplates.add(scoreEventsTemplate);
		tblTemplates.add(initScoreBalanceTemplate);
		tblTemplates.add(lastEventLeadersTemplate);

		return new GameTemplate("Casual Game", tblTemplates);
	}
}
