package betterquesting.quests;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import org.apache.logging.log4j.Level;
import betterquesting.core.BetterQuesting;
import betterquesting.network.PacketQuesting.PacketDataType;
import betterquesting.party.PartyInstance;
import betterquesting.party.PartyInstance.PartyMember;
import betterquesting.party.PartyManager;
import betterquesting.quests.rewards.RewardBase;
import betterquesting.quests.rewards.RewardRegistry;
import betterquesting.quests.tasks.TaskBase;
import betterquesting.quests.tasks.TaskRegistry;
import betterquesting.utils.BigItemStack;
import betterquesting.utils.JsonHelper;
import betterquesting.utils.NBTConverter;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class QuestInstance
{
	public int questID;
	public boolean isMain = false;
	public boolean isSilent = false;
	public boolean lockedProgress = false;
	public boolean simultaneous = false;
	public BigItemStack itemIcon = new BigItemStack(Items.nether_star);
	public ArrayList<TaskBase> tasks = new ArrayList<TaskBase>();
	public ArrayList<RewardBase> rewards = new ArrayList<RewardBase>();
	
	public String name = "New Quest";
	public String description = "No Description";
	
	ArrayList<UserEntry> completeUsers = new ArrayList<UserEntry>();
	public ArrayList<QuestInstance> preRequisites = new ArrayList<QuestInstance>();
	
	public QuestLogic logic = QuestLogic.AND;
	public QuestLogic tLogic = QuestLogic.AND;
	public boolean globalQuest = false;
	public boolean globalShare = true;
	public boolean autoClaim = false;
	public int repeatTime = -1;
	
	public QuestInstance(int questID, boolean register)
	{
		this.questID = questID;
		
		if(register)
		{
			QuestDatabase.questDB.put(this.questID, this);
		}
	}
	
	/**
	 * Quest specific living update event. Do not use for item submissions
	 * @param player
	 */
	public void Update(EntityPlayer player)
	{
		if(isComplete(player.getUniqueID()))
		{
			UserEntry entry = GetUserEntry(player.getUniqueID());
			
			if(!HasClaimed(player.getUniqueID()))
			{
				if(CanClaim(player, new NBTTagList()))
				{
					// Quest is complete and pending claim.
					// Task logic is not required to run.
					if(autoClaim && player.ticksExisted%20 == 0)
					{
						Claim(player, new NBTTagList());
					}
					
					return;
				} else if(repeatTime < 0 || rewards.size() <= 0)
				{
					// Task is non repeatable or has no rewards to claim
					return;
				} else
				{
					// Task logic will now run for repeat quest
				}
			} else if(rewards.size() > 0 && repeatTime >= 0 && player.worldObj.getTotalWorldTime() - entry.timestamp >= repeatTime)
			{
				// Task is scheduled to reset
				ResetProgress(player.getUniqueID());
				
				if(player instanceof EntityPlayerMP && !QuestDatabase.editMode && !isSilent)
				{
					NBTTagCompound tags = new NBTTagCompound();
					tags.setString("Main", "betterquesting.notice.update");
					tags.setString("Sub", name);
					tags.setInteger("Sound", 1);
					tags.setTag("Icon", itemIcon.writeToNBT(new NBTTagCompound()));
					
					if(globalQuest)
					{
						BetterQuesting.instance.network.sendToAll(PacketDataType.NOTIFICATION.makePacket(tags));
					} else if(player instanceof EntityPlayerMP)
					{
						BetterQuesting.instance.network.sendTo(PacketDataType.NOTIFICATION.makePacket(tags), (EntityPlayerMP)player);
					}
				}
				
				UpdateClients();
				return;
			} else
			{
				// No reset or reset is pending
				return;
			}
		}
		
		if(isUnlocked(player.getUniqueID()) || lockedProgress)
		{
			int done = 0;
			boolean update = false;
			
			for(TaskBase tsk : tasks)
			{
				boolean flag = !tsk.isComplete(player.getUniqueID());
				
				tsk.Update(player);
				
				if(tsk.isComplete(player.getUniqueID()))
				{
					done += 1;
					
					if(flag)
					{
						update = true;
					}
				}
			}
			
			if(!isUnlocked(player.getUniqueID()))
			{
				if(update)
				{
					UpdateClients();
				}
				
				return;
			} else if((tasks.size() > 0 || !QuestDatabase.editMode) && tLogic.GetResult(done, tasks.size()))
			{
				setComplete(player.getUniqueID(), player.worldObj.getTotalWorldTime());
				
				UpdateClients();
				
				if(!QuestDatabase.editMode && !isSilent)
				{
					NBTTagCompound tags = new NBTTagCompound();
					tags.setString("Main", "betterquesting.notice.complete");
					tags.setString("Sub", name);
					tags.setInteger("Sound", 2);
					tags.setTag("Icon", itemIcon.writeToNBT(new NBTTagCompound()));
					
					if(globalQuest)
					{
						BetterQuesting.instance.network.sendToAll(PacketDataType.NOTIFICATION.makePacket(tags));
					} else if(player instanceof EntityPlayerMP)
					{
						BetterQuesting.instance.network.sendTo(PacketDataType.NOTIFICATION.makePacket(tags), (EntityPlayerMP)player);
					}
				}
			} else if(update && simultaneous)
			{
				ResetProgress(player.getUniqueID());
				UpdateClients();
			} else if(update)
			{
				UpdateClients();
				
				if(player instanceof EntityPlayerMP && !QuestDatabase.editMode && !isSilent)
				{
					NBTTagCompound tags = new NBTTagCompound();
					tags.setString("Main", "betterquesting.notice.update");
					tags.setString("Sub", name);
					tags.setInteger("Sound", 1);
					tags.setTag("Icon", itemIcon.writeToNBT(new NBTTagCompound()));
					
					if(globalQuest)
					{
						BetterQuesting.instance.network.sendToAll(PacketDataType.NOTIFICATION.makePacket(tags));
					} else if(player instanceof EntityPlayerMP)
					{
						BetterQuesting.instance.network.sendTo(PacketDataType.NOTIFICATION.makePacket(tags), (EntityPlayerMP)player);
					}
				}
			}
		}
	}
	
	/**
	 * Fired when someone clicks the detect button for this quest
	 */
	public void Detect(EntityPlayer player)
	{
		if(isComplete(player.getUniqueID()) && (repeatTime < 0 || rewards.size() <= 0))
		{
			return;
		} else if(!canSubmit(player))
		{
			return;
		}
		
		if(isUnlocked(player.getUniqueID()) || QuestDatabase.editMode)
		{
			int done = 0;
			boolean update = false;
			
			for(TaskBase tsk : tasks)
			{
				boolean flag = !tsk.isComplete(player.getUniqueID());
				
				tsk.Detect(player);
				
				if(tsk.isComplete(player.getUniqueID()))
				{
					done += 1;
					
					if(flag)
					{
						update = true;
					}
				}
			}
			
			if((tasks.size() > 0 || !QuestDatabase.editMode) && tLogic.GetResult(done, tasks.size()))
			{
				setComplete(player.getUniqueID(), player.worldObj.getTotalWorldTime());
				
				if(player instanceof EntityPlayerMP && !QuestDatabase.editMode && !isSilent)
				{
					NBTTagCompound tags = new NBTTagCompound();
					tags.setString("Main", "betterquesting.notice.complete");
					tags.setString("Sub", name);
					tags.setInteger("Sound", 2);
					tags.setTag("Icon", itemIcon.writeToNBT(new NBTTagCompound()));
					
					if(globalQuest)
					{
						BetterQuesting.instance.network.sendToAll(PacketDataType.NOTIFICATION.makePacket(tags));
					} else if(player instanceof EntityPlayerMP)
					{
						BetterQuesting.instance.network.sendTo(PacketDataType.NOTIFICATION.makePacket(tags), (EntityPlayerMP)player);
					}
				}
			} else if(update && simultaneous)
			{
				ResetProgress(player.getUniqueID());
				UpdateClients();
			} else if(update)
			{
				if(player instanceof EntityPlayerMP && !QuestDatabase.editMode && !isSilent)
				{
					NBTTagCompound tags = new NBTTagCompound();
					tags.setString("Main", "betterquesting.notice.update");
					tags.setString("Sub", name);
					tags.setInteger("Sound", 1);
					tags.setTag("Icon", itemIcon.writeToNBT(new NBTTagCompound()));
					
					if(globalQuest)
					{
						BetterQuesting.instance.network.sendToAll(PacketDataType.NOTIFICATION.makePacket(tags));
					} else if(player instanceof EntityPlayerMP)
					{
						BetterQuesting.instance.network.sendTo(PacketDataType.NOTIFICATION.makePacket(tags), (EntityPlayerMP)player);
					}
				}
			}
			
			UpdateClients();
		}
	}
	
	public boolean HasClaimed(UUID uuid)
	{
		if(rewards.size() <= 0)
		{
			return true;
		}
		
		if(globalQuest && !globalShare)
		{
			for(UserEntry entry : completeUsers)
			{
				if(entry.claimed)
				{
					return true;
				}
			}
			
			return false;
		}
		
		UserEntry entry = GetUserEntry(uuid);
		
		if(entry == null)
		{
			return false;
		}
		
		return entry.claimed;
	}
	
	public boolean CanClaim(EntityPlayer player, NBTTagList choiceData)
	{
		UserEntry entry = GetUserEntry(player.getUniqueID());
		
		if(entry == null || HasClaimed(player.getUniqueID()))
		{
			return false;
		} else if(canSubmit(player))
		{
			return false;
		} else
		{
			for(int i = 0; i < rewards.size(); i++)
			{
				RewardBase rew = rewards.get(i);
				
				NBTTagCompound cTag = new NBTTagCompound();
				
				if(choiceData != null && choiceData.tagCount() > i)
				{
					cTag = choiceData.getCompoundTagAt(i);
				}
				
				if(!rew.canClaim(player, cTag))
				{
					return false;
				}
			}
		}
		
		return true;
	}
	
	public void Claim(EntityPlayer player, NBTTagList choiceData)
	{
		for(int i = 0; i < rewards.size(); i++)
		{
			RewardBase rew = rewards.get(i);
			
			NBTTagCompound cTag = new NBTTagCompound();
			
			if(choiceData != null && choiceData.tagCount() > i)
			{
				cTag = choiceData.getCompoundTagAt(i);
			}
			
			rew.Claim(player, cTag);
		}
		
		UserEntry entry = GetUserEntry(player.getUniqueID());
		entry.claimed = true;
		entry.timestamp = player.worldObj.getTotalWorldTime();
		
		UpdateClients();
	}
	
	public boolean canSubmit(EntityPlayer player)
	{
		if(player == null)
		{
			return false;
		}
		
		UserEntry entry = this.GetUserEntry(player.getUniqueID());
		
		if(entry == null)
		{
			return true;
		} else if(!entry.claimed)
		{
			int done = 0;
			
			for(TaskBase tsk : tasks)
			{
				if(tsk.isComplete(player.getUniqueID()))
				{
					done += 1;
				}
			}
			
			return !tLogic.GetResult(done, tasks.size());
		} else
		{
			return false;
		}
	}
	
	@SideOnly(Side.CLIENT)
	public NBTTagList GetChoiceData()
	{
		NBTTagList cList = new NBTTagList();
		
		for(RewardBase rew : rewards)
		{
			cList.appendTag(rew.GetChoiceData());
		}
		
		return cList;
	}
	
	@SideOnly(Side.CLIENT)
	public void SetChoiceData(NBTTagList tags)
	{
		for(int i = 0; i < rewards.size(); i++)
		{
			if(i >= tags.tagCount())
			{
				break;
			}
			
			RewardBase rew = rewards.get(i);
			rew.SetChoiceData(tags.getCompoundTagAt(i));
		}
	}
	
	@SideOnly(Side.CLIENT)
	public ArrayList<String> getStandardTooltip(EntityPlayer player)
	{
		ArrayList<String> list = new ArrayList<String>();
		
		list.add(StatCollector.translateToLocalFormatted(name));
		
		if(isComplete(player.getUniqueID()))
		{
			list.add(EnumChatFormatting.GREEN + StatCollector.translateToLocalFormatted("betterquesting.tooltip.complete"));
			
			if(!HasClaimed(player.getUniqueID()))
			{
				list.add(EnumChatFormatting.GRAY + StatCollector.translateToLocalFormatted("betterquesting.tooltip.rewards_pending"));
			} else if(repeatTime > 0)
			{
				long time = getRepeatSeconds(player);
				DecimalFormat df = new DecimalFormat("00");
				list.add(EnumChatFormatting.GRAY + StatCollector.translateToLocalFormatted("betterquesting.tooltip.repeat_time", (time/60) + "m " + df.format(time%60) + "s"));
			}
		} else if(!isUnlocked(player.getUniqueID()))
		{
			list.add(EnumChatFormatting.RED + "" + EnumChatFormatting.UNDERLINE + StatCollector.translateToLocalFormatted("betterquesting.tooltip.requires") + " (" + logic.toString().toUpperCase() + ")");
			
			for(QuestInstance req : preRequisites)
			{
				if(!req.isComplete(player.getUniqueID()))
				{
					list.add(EnumChatFormatting.RED + "- " + StatCollector.translateToLocalFormatted(req.name));
				}
			}
		} else
		{
			int n = 0;
			
			for(TaskBase task : tasks)
			{
				if(task.isComplete(player.getUniqueID()))
				{
					n++;
				}
			}
			
			list.add(EnumChatFormatting.GRAY + StatCollector.translateToLocalFormatted("betterquesting.tooltip.tasks_complete", n, tasks.size()));
		}
		
		list.add(EnumChatFormatting.DARK_GRAY + StatCollector.translateToLocalFormatted("betterquesting.tooltip.shift_advanced"));
		
		return list;
	}
	
	@SideOnly(Side.CLIENT)
	public ArrayList<String> getAdvancedTooltip(EntityPlayer player)
	{
		ArrayList<String> list = new ArrayList<String>();
		
		list.add(StatCollector.translateToLocalFormatted(name));
		
		list.add(EnumChatFormatting.GRAY + StatCollector.translateToLocalFormatted("betterquesting.tooltip.main_quest", isMain));
		list.add(EnumChatFormatting.GRAY + StatCollector.translateToLocalFormatted("betterquesting.tooltip.global_quest", globalQuest));
		if(globalQuest)
		{
			list.add(EnumChatFormatting.GRAY + StatCollector.translateToLocalFormatted("betterquesting.tooltip.global_share", globalShare));
		}
		list.add(EnumChatFormatting.GRAY + StatCollector.translateToLocalFormatted("betterquesting.tooltip.task_logic", logic.toString().toUpperCase()));
		list.add(EnumChatFormatting.GRAY + StatCollector.translateToLocalFormatted("betterquesting.tooltip.simultaneous", simultaneous));
		list.add(EnumChatFormatting.GRAY + StatCollector.translateToLocalFormatted("betterquesting.tooltip.auto_claim", autoClaim));
		if(repeatTime >= 0)
		{
			DecimalFormat df = new DecimalFormat("00");
			list.add(EnumChatFormatting.GRAY + StatCollector.translateToLocalFormatted("betterquesting.tooltip.repeat", (repeatTime/60) + "m " + df.format(repeatTime%60) + "s"));
		} else
		{
			list.add(EnumChatFormatting.GRAY + StatCollector.translateToLocalFormatted("betterquesting.tooltip.repeat", false));
		}
		
		return list;
	}
	
	@SideOnly(Side.CLIENT)
	public long getRepeatSeconds(EntityPlayer player)
	{
		if(repeatTime < 0)
		{
			return -1;
		}
		
		UserEntry ue = GetUserEntry(player.getUniqueID());
		
		if(ue == null)
		{
			return 0;
		} else
		{
			return (repeatTime - (player.worldObj.getTotalWorldTime() - ue.timestamp))/20L;
		}
	}
	
	public void UpdateClients()
	{
		NBTTagCompound tags = new NBTTagCompound();
		tags.setInteger("questID", this.questID);
		JsonObject json = new JsonObject();
		writeToJSON(json);
		tags.setTag("Data", NBTConverter.JSONtoNBT_Object(json, new NBTTagCompound()));
		BetterQuesting.instance.network.sendToAll(PacketDataType.QUEST_SYNC.makePacket(tags));
	}
	
	public boolean isUnlocked(UUID uuid)
	{
		int A = 0;
		int B = preRequisites.size();
		
		if(B <= 0)
		{
			return true;
		}
		
		for(QuestInstance quest : preRequisites)
		{
			if(quest != null && quest.isComplete(uuid))
			{
				A++;
			}
		}
		
		return logic.GetResult(A, B);
	}
	
	public void setComplete(UUID uuid, long timestamp)
	{
		PartyInstance party = PartyManager.GetParty(uuid);
		
		if(party == null)
		{
			UserEntry entry = this.GetUserEntry(uuid);
			
			if(entry != null)
			{
				entry.claimed = false;
				entry.timestamp = timestamp;
			} else
			{
				completeUsers.add(new UserEntry(uuid, timestamp));
			}
		} else
		{
			for(PartyMember mem : party.GetMembers())
			{
				UserEntry entry = this.GetUserEntry(mem.userID);
				
				if(entry != null)
				{
					entry.claimed = false;
					entry.timestamp = timestamp;
				} else
				{
					completeUsers.add(new UserEntry(mem.userID, timestamp));
				}
			}
		}
	}
	
	/**
	 * Returns true if the quest has been completed at least once
	 * @param uuid
	 * @return
	 */
	public boolean isComplete(UUID uuid)
	{
		if(this.globalQuest)
		{
			return completeUsers.size() > 0;
		} else
		{
			return GetUserEntry(uuid) != null;
		}
	}
	
	public void RemoveUserEntry(UUID... uuid)
	{
		for(int i = completeUsers.size() - 1; i >= 0; i--)
		{
			UserEntry entry = completeUsers.get(i);
			
			if(entry.uuid.equals(uuid))
			{
				completeUsers.remove(i);
				UpdateClients();
			}
		}
	}
	
	public UserEntry GetUserEntry(UUID uuid)
	{
		for(UserEntry entry : completeUsers)
		{
			if(entry.uuid.equals(uuid))
			{
				return entry;
			}
		}
		
		return null;
	}
	
	/**
	 * Clears all quest data and completion states
	 */
	public void ResetQuest()
	{
		this.completeUsers = new ArrayList<UserEntry>();
		
		for(TaskBase t : tasks)
		{
			t.ResetAllProgress();
		}
	}
	
	/**
	 * Resets task progress and claim status but does not reset completion status (applies to party members too)
	 */
	public void ResetProgress(UUID uuid)
	{
		PartyInstance party = PartyManager.GetParty(uuid);
		
		if(party == null)
		{
			UserEntry entry = GetUserEntry(uuid);
			
			if(entry != null)
			{
				entry.claimed = false;
			}
			
			for(TaskBase t : tasks)
			{
				t.ResetProgress(uuid);
			}
		} else
		{
			for(PartyMember mem : party.GetMembers())
			{
				UserEntry entry = GetUserEntry(mem.userID);
				
				if(entry != null)
				{
					entry.claimed = false;
				}
				
				for(TaskBase t : tasks)
				{
					t.ResetProgress(mem.userID);
				}
			}
		}
	}
	
	public void AddPreRequisite(QuestInstance quest)
	{
		if(!this.preRequisites.contains(quest.questID))
		{
			this.preRequisites.add(quest);
		}
	}
	
	public void RemovePreRequisite(QuestInstance quest)
	{
		this.preRequisites.remove(quest);
	}
	
	public void writeToJSON(JsonObject jObj)
	{
		jObj.addProperty("questID", questID);
		
		jObj.addProperty("name", name);
		jObj.addProperty("description", description);
		jObj.addProperty("isMain", isMain);
		jObj.addProperty("isSilent", isSilent);
		jObj.addProperty("lockedProgress", lockedProgress);
		jObj.addProperty("simultaneous", simultaneous);
		jObj.addProperty("globalQuest", globalQuest);
		jObj.addProperty("globalShare", globalShare);
		jObj.addProperty("autoClaim", autoClaim);
		jObj.addProperty("repeatTime", repeatTime);
		jObj.addProperty("logic", logic.toString());
		jObj.addProperty("taskLogic", tLogic.toString());
		jObj.add("icon", JsonHelper.ItemStackToJson(itemIcon, new JsonObject()));
		
		JsonArray tskJson = new JsonArray();
		for(TaskBase quest : tasks)
		{
			String taskID = TaskRegistry.GetID(quest.getClass());
			
			if(taskID == null)
			{
				BetterQuesting.logger.log(Level.ERROR, "A quest was unable to save an unregistered task: " + quest.getClass().getName());
				continue;
			}
			
			JsonObject qJson = new JsonObject();
			quest.writeToJson(qJson);
			qJson.addProperty("taskID", TaskRegistry.GetID(quest.getClass()));
			tskJson.add(qJson);
		}
		jObj.add("tasks", tskJson);
		
		JsonArray rwdJson = new JsonArray();
		for(RewardBase rew : rewards)
		{
			JsonObject rJson = new JsonObject();
			rew.writeToJson(rJson);
			rJson.addProperty("rewardID", RewardRegistry.GetID(rew.getClass()));
			rwdJson.add(rJson);
		}
		jObj.add("rewards", rwdJson);
		
		JsonArray comJson = new JsonArray();
		for(UserEntry entry : completeUsers)
		{
			comJson.add(entry.toJson());
		}
		jObj.add("completed", comJson);
		
		JsonArray reqJson = new JsonArray();
		for(QuestInstance quest : preRequisites)
		{
			if(quest == null)
			{
				BetterQuesting.logger.log(Level.ERROR, "Quest " + name + " had null prequisite!", new IllegalArgumentException());
				continue;
			}
			reqJson.add(new JsonPrimitive(quest.questID));
		}
		jObj.add("preRequisites", reqJson);
	}
	
	public void readFromJSON(JsonObject jObj)
	{
		this.questID = JsonHelper.GetNumber(jObj, "questID", -1).intValue();
		
		this.name = JsonHelper.GetString(jObj, "name", "New Quest");
		this.description = JsonHelper.GetString(jObj, "description", "No Description");
		this.isMain = JsonHelper.GetBoolean(jObj, "isMain", false);
		this.isSilent = JsonHelper.GetBoolean(jObj, "isSilent", false);
		this.lockedProgress = JsonHelper.GetBoolean(jObj, "lockedProgress", false);
		this.simultaneous = JsonHelper.GetBoolean(jObj, "simultaneous", false);
		this.globalQuest = JsonHelper.GetBoolean(jObj, "globalQuest", false);
		this.globalShare = JsonHelper.GetBoolean(jObj, "globalShare", true);
		this.autoClaim = JsonHelper.GetBoolean(jObj, "autoClaim", false);
		this.repeatTime = JsonHelper.GetNumber(jObj, "repeatTime", -1).intValue();
		try
		{
			this.logic = QuestLogic.valueOf(JsonHelper.GetString(jObj, "logic", "AND").toUpperCase());
			this.logic = logic == null? QuestLogic.AND : logic;
		} catch(Exception e)
		{
			this.logic = QuestLogic.AND;
		}
		try
		{
			this.tLogic = QuestLogic.valueOf(JsonHelper.GetString(jObj, "taskLogic", "AND").toUpperCase());
			this.tLogic = tLogic == null? QuestLogic.AND : tLogic;
		} catch(Exception e)
		{
			this.tLogic = QuestLogic.AND;
		}
		this.itemIcon = JsonHelper.JsonToItemStack(JsonHelper.GetObject(jObj, "icon"));
		this.itemIcon = this.itemIcon != null? this.itemIcon : new BigItemStack(Items.nether_star);
		
		this.tasks = new ArrayList<TaskBase>();
		for(JsonElement entry : JsonHelper.GetArray(jObj, "tasks"))
		{
			if(entry == null || !entry.isJsonObject())
			{
				continue;
			}
			
			JsonObject jsonQuest = entry.getAsJsonObject();
			TaskBase quest = TaskRegistry.InstatiateTask(JsonHelper.GetString(jsonQuest, "taskID", ""));
			
			if(quest != null)
			{
				quest.readFromJson(jsonQuest);
				this.tasks.add(quest);
			}
		}
		
		this.rewards = new ArrayList<RewardBase>();
		for(JsonElement entry : JsonHelper.GetArray(jObj, "rewards"))
		{
			if(entry == null || !entry.isJsonObject())
			{
				continue;
			}
			
			JsonObject jsonReward = entry.getAsJsonObject();
			RewardBase reward = RewardRegistry.InstatiateReward(JsonHelper.GetString(jsonReward, "rewardID", ""));
			
			if(reward != null)
			{
				reward.readFromJson(jsonReward);
				this.rewards.add(reward);
			}
		}
		
		completeUsers = new ArrayList<UserEntry>();
		for(JsonElement entry : JsonHelper.GetArray(jObj, "completed"))
		{
			if(entry == null || !entry.isJsonObject())
			{
				continue;
			}
			
			try
			{
				UUID uuid = UUID.fromString(JsonHelper.GetString(entry.getAsJsonObject(), "uuid", ""));
				UserEntry user = new UserEntry(uuid);
				user.fromJson(entry.getAsJsonObject());
				completeUsers.add(user);
			} catch(Exception e)
			{
				BetterQuesting.logger.log(Level.ERROR, "Unable to load UUID for quest", e);
			}
		}
		
		preRequisites = new ArrayList<QuestInstance>();
		for(JsonElement entry : JsonHelper.GetArray(jObj, "preRequisites"))
		{
			if(entry == null || !entry.isJsonPrimitive() || !entry.getAsJsonPrimitive().isNumber())
			{
				continue;
			}
			
			preRequisites.add(QuestDatabase.GetOrRegisterQuest(entry.getAsInt()));
		}
	}
	
	private class UserEntry
	{
		public final UUID uuid;
		public long timestamp = 0;
		public boolean claimed = false;
		
		public UserEntry(UUID uuid, long timestamp)
		{
			this(uuid);
			this.timestamp = timestamp;
		}
		
		public UserEntry(UUID uuid)
		{
			this.uuid = uuid;
		}
		
		public JsonObject toJson()
		{
			JsonObject json = new JsonObject();
			json.addProperty("uuid", uuid.toString());
			json.addProperty("timestamp", timestamp);
			json.addProperty("claimed", claimed);
			return json;
		}
		
		public void fromJson(JsonObject json)
		{
			timestamp = JsonHelper.GetNumber(json, "timestamp", 0).longValue();
			claimed = JsonHelper.GetBoolean(json, "claimed", false);
		}
	}
	
	public enum QuestLogic
	{
		AND, // All complete
		NAND, // Any incomplete
		OR, // Any complete
		NOR, // All incomplete
		XOR, // Only one complete
		XNOR; // Only one incomplete
		
		public boolean GetResult(int inputs, int total)
		{
			switch(this)
			{
				case AND:
					return inputs == total;
				case NAND:
					return inputs < total;
				case NOR:
					return inputs == 0; 
				case OR:
					return inputs > 0;
				case XNOR:
					return inputs == total - 1;
				case XOR:
					return inputs == 1;
				default:
					return false;
			}
		}
	}
}