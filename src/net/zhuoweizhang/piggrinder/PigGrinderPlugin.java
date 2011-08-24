package net.zhuoweizhang.piggrinder;import java.util.*;import org.bukkit.*;import org.bukkit.command.*;
import org.bukkit.event.*;import org.bukkit.entity.*;import org.bukkit.inventory.*;
import org.bukkit.plugin.*;import org.bukkit.plugin.java.*;import org.bukkit.util.config.*;
import org.bukkit.event.player.*;import org.getspout.spoutapi.*;import org.getspout.spoutapi.player.*;
public class PigGrinderPlugin extends JavaPlugin {public Recipe grinderRecipe; 
public Material grinderMaterial = Material.BUCKET;public short grinderMetadata = 70;public int grinderDelay = 5;
public int grinderAmount = 50;public boolean grinderExplode = true;public float grinderExplodePower = 1f;
public String grinderTextureURL;public double grinderYVelocity;
public final PigGrinderPlayerListener playerListener = new PigGrinderPlayerListener(this);
public final List<PigGrinderTask> tasks = new ArrayList<PigGrinderTask>();public void onEnable() {
Configuration config = getConfiguration();Material mat = Material.matchMaterial(config.getString("material", "BUCKET"));
if (mat != null)grinderMaterial = mat;grinderMetadata = (short) config.getInt("metadata", 70);
grinderDelay = config.getInt("delay", 5);grinderAmount = config.getInt("amount", 20);
grinderExplode = config.getBoolean("explode", true);grinderExplodePower = (float) config.getDouble("explodepower", 1.0);
grinderTextureURL = config.getString("textureurl", "http://cloud.github.com/downloads/zhuowei/PigGrinder/pig_grinder_texture.png");
grinderYVelocity = config.getDouble("yvelocity", 0.25);config.save();PluginManager pm = this.getServer().getPluginManager();
pm.registerEvent(Event.Type.PLAYER_INTERACT_ENTITY, playerListener, Event.Priority.Normal, this);
grinderRecipe = new ShapedRecipe(new ItemStack(grinderMaterial, 1, grinderMetadata)).shape("bib", "iri", "bib").
setIngredient('b', Material.CLAY_BRICK).setIngredient('i', Material.IRON_INGOT).setIngredient('r', Material.REDSTONE);
getServer().addRecipe(grinderRecipe);try {SpoutManager.getItemManager().setItemName(grinderMaterial, grinderMetadata, "Grinder");}
catch(NoClassDefFoundError err) {System.err.println("[PigGrinder] Spout is not installed. ");}catch(Exception e) {
System.err.println("[PigGrinder] Could not initialize Spout support.");}}public void onDisable() {
getServer().getScheduler().cancelTasks(this);}public PigGrinderTask grind(Pig pig) {
PigGrinderTask task = new PigGrinderTask(pig, grinderAmount);int taskId = getServer().getScheduler().
scheduleSyncRepeatingTask(this, task, grinderDelay, grinderDelay);task.taskId = taskId;tasks.add(task);
return task;}public class PigGrinderTask implements Runnable {public Pig pig;public int amount;
public int taskId = 0;public PigGrinderTask(Pig pig, int amount) {this.pig = pig;this.amount = amount;
try {AppearanceManager manager = SpoutManager.getAppearanceManager();for (Player p: pig.getServer().
getOnlinePlayers()) {manager.setEntitySkin(SpoutManager.getPlayer(p), pig, grinderTextureURL);}}
catch(NoClassDefFoundError err) {}}public void run() {if (pig.isDead()) {Bukkit.getServer().getScheduler().
cancelTask(taskId);if (tasks.contains(this)) {tasks.remove(this);}return;}Material pigDrop = (pig.getFireTicks() > 0 ?
 Material.GRILLED_PORK : Material.PORK);World world = pig.getWorld();Location loc = pig.getLocation();
Item item = world.dropItemNaturally(loc, new ItemStack(pigDrop, 1));item.setVelocity(item.getVelocity().
setY(item.getVelocity().getY() + grinderYVelocity));amount--;if (amount <= 0) {pig.remove();if (grinderExplode)
 {world.createExplosion(loc, grinderExplodePower);}Bukkit.getServer().getScheduler().cancelTask(taskId);
if (tasks.contains(this)) {tasks.remove(this);}}}}private class PigGrinderPlayerListener extends PlayerListener {
public PigGrinderPlugin plugin;public PigGrinderPlayerListener(PigGrinderPlugin plugin) {this.plugin = plugin;}
public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {if (event.isCancelled() ||
 !(event.getRightClicked() instanceof Pig)) {return;}ItemStack tool = event.getPlayer().getItemInHand();
if (tool == null || !tool.getType().equals(plugin.grinderMaterial) || tool.getDurability() != plugin.grinderMetadata)
 {return;}Pig pig = (Pig) event.getRightClicked();for (PigGrinderPlugin.PigGrinderTask task: plugin.tasks) {
if (task.pig.equals(pig)) {return;}}event.setCancelled(true);event.getPlayer().setItemInHand(new ItemStack(
plugin.grinderMaterial, tool.getAmount() - 1, plugin.grinderMetadata));plugin.grind(pig);}}}
