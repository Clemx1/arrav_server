package net.edge.world.node.entity.player;

import com.google.gson.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.edge.content.combat.weapon.FightType;
import net.edge.content.container.ItemContainer;
import net.edge.content.minigame.barrows.BarrowsData;
import net.edge.content.pets.Pet;
import net.edge.content.pets.PetManager;
import net.edge.content.pets.PetProgress;
import net.edge.content.quest.Quest;
import net.edge.content.quest.QuestManager.Quests;
import net.edge.content.skill.Skill;
import net.edge.content.skill.Skills;
import net.edge.content.skill.slayer.Slayer;
import net.edge.content.skill.summoning.Summoning;
import net.edge.content.skill.summoning.familiar.Familiar;
import net.edge.content.skill.summoning.familiar.FamiliarContainer;
import net.edge.locale.Position;
import net.edge.net.codec.login.LoginResponse;
import net.edge.util.json.GsonUtils;
import net.edge.world.World;
import net.edge.world.node.entity.attribute.AttributeKey;
import net.edge.world.node.entity.attribute.AttributeValue;
import net.edge.world.node.entity.player.assets.AntifireDetails;
import net.edge.world.node.entity.player.assets.PrayerBook;
import net.edge.world.node.entity.player.assets.Rights;
import net.edge.world.node.entity.player.assets.Spellbook;
import net.edge.world.node.item.Item;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;

import static net.edge.net.codec.login.LoginResponse.ACCOUNT_DISABLED;
import static net.edge.net.codec.login.LoginResponse.COULD_NOT_COMPLETE_LOGIN;
import static net.edge.net.codec.login.LoginResponse.INVALID_CREDENTIALS;

/**
 * The serializer that will serialize and deserialize character files for
 * players.
 * <p>
 * <p>
 * Serialization of character files can and should be done on another thread
 * whenever possible to avoid doing disk I/O on the main game thread.
 * @author lare96 <http://github.com/lare96>
 */
public final class PlayerSerialization {
	
	/**
	 * The player this serializer is dedicated to.
	 */
	private final Player player;
	
	/**
	 * The character file that corresponds to this player.
	 */
	private final File cf;
	
	/**
	 * Creates a new {@link PlayerSerialization}.
	 * @param player the player this serializer is dedicated to.
	 */
	public PlayerSerialization(Player player) {
		this.player = player;
		this.cf = Paths.get("./data/players/" + player.getUsername() + ".json").toFile();
	}
	
	/**
	 * Serializes the dedicated player into a {@code JSON} file.
	 */
	public void serialize() {
		try {
			cf.getParentFile().setWritable(true);
			if(!cf.getParentFile().exists()) {
				try {
					cf.getParentFile().mkdirs();
				} catch(SecurityException e) {
					throw new IllegalStateException("Unable to register directory for character files!");
				}
			}
			try(FileWriter out = new FileWriter(cf)) {
				Gson gson = new GsonBuilder().setPrettyPrinting().create();
				JsonObject obj = new JsonObject();
				for(Token token : TOKENS) {
					obj.add(token.getName(), gson.toJsonTree(token.toJson(player)));
				}
				Object2ObjectArrayMap<String, Object> quests = new Object2ObjectArrayMap<>();
				for(Map.Entry<Quests, Quest> it : player.getQuestManager().getStartedQuests().entrySet()) {
					Quests key = it.getKey();
					Quest value = it.getValue();
					Object2ObjectLinkedOpenHashMap<String, Object> attributeEntry = new Object2ObjectLinkedOpenHashMap<>();
					attributeEntry.put("quest", value);
					quests.put(key.name(), attributeEntry);
				}
				obj.add("quests", gson.toJsonTree(quests));
				Object2ObjectArrayMap<String, Object> attributes = new Object2ObjectArrayMap<>();
				for(Map.Entry<String, AttributeValue<?>> it : player.getAttr()) {
					AttributeKey<?> key = AttributeKey.ALIASES.get(it.getKey());
					AttributeValue<?> value = it.getValue();
					
					if(key.isPersistent()) {
						Object2ObjectLinkedOpenHashMap<String, Object> attributeEntry = new Object2ObjectLinkedOpenHashMap<>();
						attributeEntry.put("type", key.getTypeName());
						attributeEntry.put("value", value.get());
						attributes.put(key.getName(), attributeEntry);
					}
				}
				obj.add("attributes", gson.toJsonTree(attributes));
				out.write(gson.toJson(obj));
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Initializes the decoder and gives out a {@link LoginResponse}.
	 * @param password the password used to connect.
	 * @return login response with the encoded json object.
	 */
	public SerializeResponse loginCheck(String password) {
		SerializeResponse response = new SerializeResponse(LoginResponse.NORMAL);
		try {
			File file = Paths.get("./data/players/" + player.getUsername() + ".json").toFile();
			if(!file.exists()) {
				return response;
			}
			cf.setReadable(true);
			try(FileReader in = new FileReader(cf)) {
				JsonElement read = new JsonParser().parse(in);
				JsonObject decoded = (JsonObject) read;
				if(decoded.isJsonNull()) {
					response.setResponse(COULD_NOT_COMPLETE_LOGIN);
					response.setReader(null);
					return response;
				}
				response.setReader(decoded);
				if(decoded.has("password")) {
					String realPassword = decoded.get("password").getAsString();
					if(!password.equals(realPassword) && password.length() > 0) {
						response.setResponse(INVALID_CREDENTIALS);
						response.setReader(null);
						return response;
					}
				}
				if(decoded.has("banned")) {
					if(decoded.get("banned").getAsBoolean()) {
						response.setResponse(ACCOUNT_DISABLED);
						response.setReader(null);
						return response;
					}
				}
				if(decoded.has("rights")) {
					player.setRights(Rights.valueOf(decoded.get("rights").getAsString()));
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
			response.setResponse(COULD_NOT_COMPLETE_LOGIN);
			response.setReader(null);
			return response;
		}
		return response;
	}
	
	/**
	 * Deserializes the player from a {@code JSON} file.
	 */
	public void deserialize(JsonObject reader) {
		try {
			if(reader == null) {
				Skills.create(player);
				return;
			}
			Gson gson = new GsonBuilder().create();
			for(Token token : TOKENS) {
				if(reader.has(token.getName()))
					token.fromJson(gson, player, reader.get(token.getName()));
			}
			if(reader.has("attributes")) {
				JsonObject attr = reader.get("attributes").getAsJsonObject();
				for(Map.Entry<String, JsonElement> it : attr.entrySet()) {
					JsonObject obj = it.getValue().getAsJsonObject();
					String old = obj.get("type").getAsString();
					Class<?> type = Class.forName(old);
					Object data = GsonUtils.getAsType(obj.get("value"), type);
					if(AttributeKey.ALIASES.keySet().stream().anyMatch(s -> s.equals(it.getKey())))
						player.getAttr().get(it.getKey()).set(data);
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static Token bank(int index) {
		return new Token("bank" + index) {
			@Override
			public Object toJson(Player p) {
				return p.getBank().items(index);
			}
			@Override
			public void fromJson(Gson b, Player p, JsonElement n) {
				p.getBank().setItems(index, b.fromJson(n, Item[].class));
			}
		};
	}
	
	private static final Token[] TOKENS = {new Token("username") {
		@Override
		public Object toJson(Player p) {
			return p.getUsername();
		}
		
		@Override
		public void fromJson(Gson b, Player p, JsonElement n) {
			p.setUsername(n.getAsString());
		}
	}, new Token("password") {
		@Override
		public Object toJson(Player p) {
			return p.getPassword();
		}
		
		@Override
		public void fromJson(Gson b, Player p, JsonElement n) {
			p.setPassword(n.getAsString());
		}
	}, new Token("banned") {
		@Override
		public Object toJson(Player p) {
			return p.isBanned();
		}
		
		@Override
		public void fromJson(Gson b, Player p, JsonElement n) {
			p.setBanned(n.getAsBoolean());
		}
	}, new Token("muted") {
		@Override
		public Object toJson(Player p) {
			return p.isMuted();
		}
		
		@Override
		public void fromJson(Gson b, Player p, JsonElement n) {
			p.setMuted(n.getAsBoolean());
		}
	}, new Token("position") {
		@Override
		public Object toJson(Player p) {
			return p.getPosition();
		}
		
		@Override
		public void fromJson(Gson b, Player p, JsonElement n) {
			p.setPosition(b.fromJson(n, Position.class));
		}
	}, new Token("rights") {
		@Override
		public Object toJson(Player p) {
			return p.getRights();
		}
		
		@Override
		public void fromJson(Gson b, Player p, JsonElement n) {
			p.setRights(Rights.valueOf(n.getAsString()));
		}
	}, new Token("nightmare") {
		@Override
		public Object toJson(Player p) {
			return p.getNightMode();
		}
		
		@Override
		public void fromJson(Gson b, Player p, JsonElement n) {
			p.setNight(n.getAsInt());
		}
	}, new Token("totalVotes") {

		@Override
		public Object toJson(Player p) {
			return p.getTotalVotes();
		}

		@Override
		public void fromJson(Gson b, Player p, JsonElement n) {
			p.setTotalVotes(n.getAsInt());
		}
	}, new Token("vote") {
		@Override
		public Object toJson(Player p) {
			return p.getVotePoints();
		}
		
		@Override
		public void fromJson(Gson b, Player p, JsonElement n) {
			p.setVotePoints(n.getAsInt());
		}
	}, new Token("clan") {
		@Override
		public Object toJson(Player p) {
			return p.getClan().map(clanMember -> clanMember.getClan().getOwner()).orElse("");
		}
		
		@Override
		public void fromJson(Gson b, Player p, JsonElement n) {
			World.getClanManager().join(p, n.getAsString());
		}
	}, new Token("appearance") {
		@Override
		public Object toJson(Player p) {
			return p.getAppearance().getValues();
		}
		
		@Override
		public void fromJson(Gson b, Player p, JsonElement n) {
			p.getAppearance().setValues(b.fromJson(n, int[].class));
		}
	}, new Token("prayer-type") {
		@Override
		public Object toJson(Player p) {
			return p.getPrayerBook();
		}
		
		@Override
		public void fromJson(Gson b, Player p, JsonElement n) {
			p.setPrayerBook(PrayerBook.valueOf(n.getAsString()));
		}
	}, new Token("spell-book") {
		@Override
		public Object toJson(Player p) {
			return p.getSpellbook();
		}
		
		@Override
		public void fromJson(Gson b, Player p, JsonElement n) {
			p.setSpellbook(Spellbook.valueOf(n.getAsString()));
		}
	}, new Token("fight-type") {
		@Override
		public Object toJson(Player p) {
			return p.getFightType();
		}
		
		@Override
		public void fromJson(Gson b, Player p, JsonElement n) {
			p.setFightType(FightType.valueOf(n.getAsString()));
		}
	}, new Token("poison-damage") {
		@Override
		public Object toJson(Player p) {
			return p.getPoisonDamage().get();
		}
		
		@Override
		public void fromJson(Gson b, Player p, JsonElement n) {
			p.getPoisonDamage().set(n.getAsInt());
		}
	}, new Token("auto-retaliate") {
		@Override
		public Object toJson(Player p) {
			return p.isAutoRetaliate();
		}
		
		@Override
		public void fromJson(Gson b, Player p, JsonElement n) {
			p.setAutoRetaliate(n.getAsBoolean());
		}
	}, new Token("running") {
		@Override
		public Object toJson(Player p) {
			return p.getMovementQueue().isRunning();
		}
		
		@Override
		public void fromJson(Gson b, Player p, JsonElement n) {
			p.getMovementQueue().setRunning(n.getAsBoolean());
		}
	}, new Token("antifire-details") {
		@Override
		public Object toJson(Player p) {
			return p.getAntifireDetails().orElse(null);
		}
		
		@Override
		public void fromJson(Gson b, Player p, JsonElement n) {
			p.setAntifireDetail(n.isJsonNull() ? Optional.empty() : Optional.of(b.fromJson(n.getAsJsonObject(), AntifireDetails.class)));
		}
	}, new Token("run-energy") {
		@Override
		public Object toJson(Player p) {
			return p.getRunEnergy();
		}
		
		@Override
		public void fromJson(Gson b, Player p, JsonElement n) {
			p.setRunEnergy(n.getAsDouble());
		}
	}, new Token("special-amount") {
		@Override
		public Object toJson(Player p) {
			return p.getSpecialPercentage().get();
		}
		
		@Override
		public void fromJson(Gson b, Player p, JsonElement n) {
			p.getSpecialPercentage().set(n.getAsInt());
		}
	}, new Token("teleblock-timer") {
		@Override
		public Object toJson(Player p) {
			return p.getTeleblockTimer().get();
		}
		
		@Override
		public void fromJson(Gson b, Player p, JsonElement n) {
			p.getTeleblockTimer().set(n.getAsInt());
		}
	}, new Token("skull-timer") {
		@Override
		public Object toJson(Player p) {
			return p.getSkullTimer().get();
		}
		
		@Override
		public void fromJson(Gson b, Player p, JsonElement n) {
			p.getSkullTimer().set(n.getAsInt());
		}
	}, new Token("vengeance") {
		@Override
		public Object toJson(Player p) {
			return p.isVenged();
		}
		
		@Override
		public void fromJson(Gson b, Player p, JsonElement n) {
			p.setVenged(n.getAsBoolean());
		}
	}, new Token("skills") {
		@Override
		public Object toJson(Player p) {
			return p.getSkills();
		}
		
		@Override
		public void fromJson(Gson b, Player p, JsonElement n) {
			Skill[] skills = p.getSkills();
			System.arraycopy(b.fromJson(n, Skill[].class), 0, skills, 0, skills.length);
		}
	}, new Token("inventory") {
		@Override
		public Object toJson(Player p) {
			return p.getInventory().toArray();
		}
		
		@Override
		public void fromJson(Gson b, Player p, JsonElement n) {
			p.getInventory().setItems(b.fromJson(n, Item[].class));
		}
	}, new Token("equipment") {
		@Override
		public Object toJson(Player p) {
			return p.getEquipment().toArray();
		}
		
		@Override
		public void fromJson(Gson b, Player p, JsonElement n) {
			p.getEquipment().setItems(b.fromJson(n, Item[].class));
		}
	}, bank(0), bank(1), bank(2), bank(3), bank(4), bank(5), bank(6), bank(7), bank(8), new Token("friends") {
		@Override
		public Object toJson(Player p) {
			return p.getFriends().toArray();
		}
		
		@Override
		public void fromJson(Gson b, Player p, JsonElement n) {
			Collections.addAll(p.getFriends(), b.fromJson(n, Long[].class));
		}
	}, new Token("ignores") {
		@Override
		public Object toJson(Player p) {
			return p.getIgnores().toArray();
		}
		
		@Override
		public void fromJson(Gson b, Player p, JsonElement n) {
			Collections.addAll(p.getIgnores(), b.fromJson(n, Long[].class));
		}
	}, new Token("total-player-kills") {
		@Override
		public Object toJson(Player p) {
			return p.getPlayerKills().get();
		}
		
		@Override
		public void fromJson(Gson b, Player p, JsonElement n) {
			p.getPlayerKills().set(n.getAsInt());
		}
	}, new Token("total-player-deaths") {
		@Override
		public Object toJson(Player p) {
			return p.getDeathsByPlayer().get();
		}
		
		@Override
		public void fromJson(Gson b, Player p, JsonElement n) {
			p.getDeathsByPlayer().set(n.getAsInt());
		}
	}, new Token("total-npc-kills") {
		@Override
		public Object toJson(Player p) {
			return p.getNpcKills().get();
		}
		
		@Override
		public void fromJson(Gson b, Player p, JsonElement n) {
			p.getNpcKills().set(n.getAsInt());
		}
	}, new Token("total-npc-deaths") {
		@Override
		public Object toJson(Player p) {
			return p.getDeathsByNpc().get();
		}
		
		@Override
		public void fromJson(Gson b, Player p, JsonElement n) {
			p.getDeathsByNpc().set(n.getAsInt());
		}
	}, new Token("total-player-kills") {
		@Override
		public Object toJson(Player p) {
			return p.getPlayerKills().get();
		}
		
		@Override
		public void fromJson(Gson b, Player p, JsonElement n) {
			p.getPlayerKills().set(n.getAsInt());
		}
	}, new Token("current-killstreak") {
		@Override
		public Object toJson(Player p) {
			return p.getCurrentKillstreak().get();
		}
		
		@Override
		public void fromJson(Gson b, Player p, JsonElement n) {
			p.getCurrentKillstreak().set(n.getAsInt());
		}
	}, new Token("highest-killstreak") {
		@Override
		public Object toJson(Player p) {
			return p.getHighestKillstreak().get();
		}
		
		@Override
		public void fromJson(Gson b, Player p, JsonElement n) {
			p.getHighestKillstreak().set(n.getAsInt());
		}
	}, new Token("godwars-killcount") {
		@Override
		public Object toJson(Player p) {
			return p.getGodwarsKillcount();
		}
		
		@Override
		public void fromJson(Gson b, Player p, JsonElement n) {
			p.setGodwarsKillcount(b.fromJson(n, int[].class));
		}
	}, new Token("killed-brothers") {
		@Override
		public Object toJson(Player p) {
			EnumSet<BarrowsData> barrows = p.getMinigameContainer().getBarrowsContainer().getKilledBrothers();
			return barrows.toArray();
		}
		
		@Override
		public void fromJson(Gson b, Player p, JsonElement n) {
			EnumSet<BarrowsData> barrows = p.getMinigameContainer().getBarrowsContainer().getKilledBrothers();
			Collections.addAll(barrows, b.fromJson(n, BarrowsData[].class));
		}
	}, new Token("slayerPoints") {
		@Override
		public Object toJson(Player p) {
			return p.getSlayerPoints();
		}
		
		@Override
		public void fromJson(Gson b, Player p, JsonElement n) {
			p.updateSlayers(n.getAsInt());
		}
	}, new Token("slayer") {
		@Override
		public Object toJson(Player p) {
			return p.getSlayer().orElse(null);
		}
		
		@Override
		public void fromJson(Gson b, Player p, JsonElement n) {
			p.setSlayer(n.isJsonNull() ? Optional.empty() : Optional.of(b.fromJson(n, Slayer.class)));
		}
	}, new Token("blocked-tasks") {
		@Override
		public Object toJson(Player p) {
			return p.getBlockedTasks();
		}
		
		@Override
		public void fromJson(Gson b, Player p, JsonElement n) {
			p.setBlockedTasks(b.fromJson(n, String[].class));
		}
	}, new Token("pets") {
		@Override
		public Object toJson(Player p) {
			return p.getPetManager().getProgress().toArray();
		}
		
		@Override
		public void fromJson(Gson b, Player p, JsonElement n) {
			Collections.addAll(p.getPetManager().getProgress(), b.fromJson(n, PetProgress[].class));
		}
	}, new Token("pet") {
		@Override
		public Object toJson(Player p) {
			PetManager pets = p.getPetManager();
			return pets.getPet().isPresent() ? pets.getPet().get().getProgress() : null;
		}
		
		@Override
		public void fromJson(Gson b, Player p, JsonElement n) {
			PetManager pets = p.getPetManager();
			pets.put(n.isJsonNull() ? null : new Pet((b.fromJson(n, PetProgress.class)), new Position(0, 0)));
		}
	}, new Token("familiar") {
		@Override
		public Object toJson(Player p) {
			Familiar familiar = p.getFamiliar().orElse(null);
			return familiar != null ? familiar.getId() : 0;
		}
		
		@Override
		public void fromJson(Gson b, Player p, JsonElement n) {
			int familiar = n.getAsInt();
			if(familiar != 0)
				p.setFamiliar(Optional.of(Summoning.FAMILIARS.get(n.getAsInt())));
		}
	}, new Token("familiar-life") {
		@Override
		public Object toJson(Player p) {
			Familiar familiar = p.getFamiliar().orElse(null);
			return familiar != null ? familiar.getCurrentHealth() : 0;
		}
		
		@Override
		public void fromJson(Gson b, Player p, JsonElement n) {
			p.getFamiliar().ifPresent(ffs -> ffs.setCurrentHealth(n.getAsInt()));
		}
	}, new Token("familiar-duration") {
		@Override
		public Object toJson(Player p) {
			Familiar familiar = p.getFamiliar().orElse(null);
			return familiar != null ? familiar.getDuration() : 0;
		}
		@Override
		public void fromJson(Gson b, Player p, JsonElement n) {
			p.getFamiliar().ifPresent(ffs -> ffs.setDuration(n.getAsInt()));
		}
	}, new Token("familiar-storage") {
		@Override
		public Object toJson(Player p) {
			Familiar familiar = p.getFamiliar().orElse(null);
			ItemContainer storage = familiar != null ? ((FamiliarContainer) familiar.getAbilityType()).getContainer() : new ItemContainer(30, ItemContainer.StackPolicy.STANDARD);
			return storage.toArray();
		}
		
		@Override
		public void fromJson(Gson b, Player p, JsonElement n) {
			p.getFamiliar()
					.ifPresent(ffs -> ((FamiliarContainer) ffs.getAbilityType()).getContainer()
							.setItems((b.fromJson(n, Item[].class))));
		}
	}};
	
	/**
	 * The container that represents a token that can be both serialized and deserialized.
	 * @author lare96 <http://github.com/lare96>
	 */
	private static abstract class Token {
		
		/**
		 * The name of this serializable token.
		 */
		private final String name;
		
		/**
		 * Creates a new {@link Token}.
		 * @param name the name of this serializable token.
		 */
		Token(String name) {
			this.name = name;
		}
		
		public abstract Object toJson(Player p);
		
		public abstract void fromJson(Gson b, Player p, JsonElement n);
		
		@Override
		public String toString() {
			return "TOKEN[name= " + name + "]";
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if(this == obj)
				return true;
			if(obj == null)
				return false;
			if(!(obj instanceof Token))
				return false;
			Token other = (Token) obj;
			if(name == null) {
				if(other.name != null)
					return false;
			} else if(!name.equals(other.name))
				return false;
			return true;
		}
		
		/**
		 * Gets the name of this serializable token.
		 * @return the name of this token.
		 */
		public String getName() {
			return name;
		}
		
	}
	
	public static class SerializeResponse {
		
		/**
		 * Login response to give away.
		 */
		private LoginResponse response;
		
		/**
		 * Saved reader for later usage.
		 */
		private JsonObject reader;
		
		public SerializeResponse(LoginResponse response) {
			this.response = response;
		}
		
		public LoginResponse getResponse() {
			return response;
		}
		
		public void setResponse(LoginResponse response) {
			this.response = response;
		}
		
		public JsonObject getReader() {
			return reader;
		}
		
		public void setReader(JsonObject reader) {
			this.reader = reader;
		}
	}
	
}