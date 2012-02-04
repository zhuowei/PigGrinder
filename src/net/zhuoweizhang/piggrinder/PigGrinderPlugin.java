package net.zhuoweizhang.piggrinder;

import java.lang.reflect.Field;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.event.Event;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import org.getspout.spoutapi.Spout;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.material.item.GenericCustomItem;
import org.getspout.spoutapi.player.EntitySkinType;

public class PigGrinderPlugin extends JavaPlugin {

	public Recipe grinderRecipe; 

	public Material grinderMaterial;

	public short grinderMetadata;

	public int grinderDelay;

	public int grinderAmount;

	public boolean grinderExplode;

	public float grinderExplodePower;

	public String grinderTextureURL, grinderCowTextureURL, grinderSheepTextureURL, grinderItemTextureURL;

	public double grinderYVelocity;

	public final PigGrinderPlayerListener playerListener = new PigGrinderPlayerListener(this);

	public final List<PigGrinderTask> tasks = new ArrayList<PigGrinderTask>();

	public boolean useSpoutItem;

	@Override
	public void onEnable() {
		FileConfiguration config = getConfig();
		config.options().copyDefaults(true);
		grinderDelay = config.getInt("delay");
		grinderAmount = config.getInt("amount");
		grinderExplode = config.getBoolean("explode");
		grinderExplodePower = (float) config.getDouble("explodepower");
		grinderTextureURL = config.getString("textureurl");
		grinderCowTextureURL = config.getString("textureurl-cow");
		grinderSheepTextureURL = config.getString("textureurl-sheep");
		grinderItemTextureURL = config.getString("itemtextureurl");
		grinderYVelocity = config.getDouble("yvelocity");
		useSpoutItem = config.getBoolean("use-spout-item");

		PluginManager pm = this.getServer().getPluginManager();
		pm.registerEvents(playerListener, this);

		try {
			//Thanks, UltraItem!
			SpoutManager.getFileManager().addToCache(this, grinderTextureURL);
			SpoutManager.getFileManager().addToCache(this, grinderCowTextureURL);
			SpoutManager.getFileManager().addToCache(this, grinderSheepTextureURL);
		}
		catch(Throwable e) {
			System.err.println("[PigGrinder] Failed to enable SpoutPlugin texture support; is it installed?");
		}

		if (useSpoutItem) {
			try {
				spoutItemInit();
			} catch (Throwable e) {
				System.err.println("[PigGrinder] Error while enabling SpoutPlugin custom item support: ");
				e.printStackTrace();
				System.err.println("[PigGrinder] Using normal item.");
				normalItemInit();
			}
		} else {
			normalItemInit();
		}
		grinderRecipe = new ShapedRecipe(new ItemStack(grinderMaterial, 1, grinderMetadata)).shape("bib", "iri", "bib").setIngredient('b', Material.CLAY_BRICK).
				setIngredient('i', Material.IRON_INGOT).setIngredient('r', Material.REDSTONE);
		getServer().addRecipe(grinderRecipe);
		this.saveConfig();
	}

	private void spoutItemInit() {
		SpoutManager.getFileManager().addToCache(this, grinderItemTextureURL);
		GenericCustomItem spoutItem = new GenericCustomItem(this, "Grinder", grinderItemTextureURL);
		grinderMaterial = Material.FLINT;
		grinderMetadata = (short) spoutItem.getCustomId();
	}

	private void normalItemInit() {
		FileConfiguration config = getConfig();
		Material mat = Material.matchMaterial(config.getString("material"));
		if (mat != null)
			grinderMaterial = mat;
		grinderMetadata = (short) config.getInt("metadata");
	}
		

	public void onDisable() {
		getServer().getScheduler().cancelTasks(this);
	}

	public PigGrinderTask grind(LivingEntity pig) {
		if (!(pig instanceof Pig || pig instanceof Cow || pig instanceof Sheep)) {
			throw new IllegalArgumentException("Only pigs, cows or sheep can be grinded");
		}
		//System.out.println("grind");
		PigGrinderTask task = new PigGrinderTask(pig, grinderAmount);
		int taskId = getServer().getScheduler().scheduleSyncRepeatingTask(this, task, grinderDelay, grinderDelay);
		task.taskId = taskId;
		tasks.add(task);
		return task;
	}

	public boolean checkCanUseGrinder(Player player, LivingEntity entity) {
		String node;
		if (entity instanceof Pig) {
			node = "piggrinder.use.pig";
		} else if (entity instanceof Sheep) {
			node = "piggrinder.use.sheep";
		} else if (entity instanceof Cow) {
			node = "piggrinder.use.cow";
		} else {
			throw new IllegalArgumentException("Only pigs, cows or sheep can be grinded");
		}
		return player.hasPermission(node);
	}

	public class PigGrinderTask implements Runnable {

		public LivingEntity pig;
		public int amount;
		public int taskId = 0;

		public PigGrinderTask(LivingEntity pig, int amount) {
			if (!(pig instanceof Pig || pig instanceof Cow || pig instanceof Sheep)) {
				throw new IllegalArgumentException("Only pigs, cows or sheep can be grinded");
			}
			this.pig = pig;
			this.amount = amount;
			try {
				String texUrl = grinderTextureURL;
				if (pig instanceof Pig) {
					texUrl = grinderTextureURL;
				} else if (pig instanceof Cow) {
					texUrl = grinderCowTextureURL;
				} else if (pig instanceof Sheep) {
					texUrl = grinderSheepTextureURL;
				}
				Spout.getServer().setEntitySkin(pig, texUrl, EntitySkinType.DEFAULT);
			}
			catch(NoClassDefFoundError err) {
				//No Spout? Shame on you.
			}
			catch(Throwable e) {
				e.printStackTrace();
			}
			if (pig instanceof Sheep) {
				((Sheep) pig).setSheared(true);
			}
		}

		public void run() {
			//System.out.println("run");
			if (pig.isDead()) {
				Bukkit.getServer().getScheduler().cancelTask(taskId);
				if (tasks.contains(this)) {
					tasks.remove(this);
				}
				return;
			}
			Material dropMat; 
			if (pig instanceof Pig) {
				dropMat = (pig.getFireTicks() > 0 ? Material.GRILLED_PORK : Material.PORK);
			} else if (pig instanceof Cow) {
				dropMat = (pig.getFireTicks() > 0 ? Material.COOKED_BEEF : Material.RAW_BEEF);
			} else if (pig instanceof Sheep) {
				dropMat = Material.WOOL;
			} else {
				dropMat = Material.AIR; //How did we get here?
			}
			World world = pig.getWorld();
			Location loc = pig.getLocation();
			Item item = world.dropItemNaturally(loc, new ItemStack(dropMat, 1));
			item.setVelocity(item.getVelocity().setY(item.getVelocity().getY() + grinderYVelocity));
			//System.out.println("dropped pork");
			amount--;

			if (amount <= 0) {
				pig.remove();
				if (grinderExplode) {
					world.createExplosion(loc, grinderExplodePower);
				}
				Bukkit.getServer().getScheduler().cancelTask(taskId);
				if (tasks.contains(this)) {
					tasks.remove(this);
				}
			}
		}
	}

}
