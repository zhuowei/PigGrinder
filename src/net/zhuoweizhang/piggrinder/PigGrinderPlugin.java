package net.zhuoweizhang.piggrinder;import java.util.*;import org.bukkit.*;import org.bukkit.command.*;
import org.bukkit.event.*;import org.bukkit.entity.*;import org.bukkit.inventory.*;
import org.bukkit.plugin.*;import org.bukkit.plugin.java.*;import org.bukkit.util.config.*;
import org.bukkit.event.player.*;import org.getspout.spoutapi.*;import org.getspout.spoutapi.player.*;
public class PigGrinderPlugin extends JavaPlugin {Recipe gRecipe; 
Material gMat;short gMetadata;int gDelay;int gAmount;boolean gExplode;float gExplodePower;
String gTexURL;double gYVel;final PGPL pListen = new PGPL(this);
final List<PGT> tasks = new ArrayList<PGT>();public void onEnable() {
Configuration config = getConfiguration();Material mat = Material.matchMaterial(config.getString(
"material", "BUCKET"));if (mat != null)gMat = mat;gMetadata = (short) config.getInt("metadata", 70);
gDelay = config.getInt("delay", 5);gAmount = config.getInt("amount", 20);
gExplode = config.getBoolean("explode", true);gExplodePower = (float) config.getDouble("explodepower", 1.0);
gTexURL = config.getString("textureurl", "http://cloud.github.com/downloads/zhuowei/PigGrinder/" +
"pig_grinder_texture.png");gYVel = config.getDouble("yvelocity", 0.25);config.save();this.getServer().
getPluginManager().registerEvent(Event.Type.PLAYER_INTERACT_ENTITY, pListen, Event.Priority.Normal, this);
gRecipe = new ShapedRecipe(new ItemStack(gMat, 1, gMetadata)).shape("bib", "iri", "bib").
setIngredient('b', Material.CLAY_BRICK).setIngredient('i', Material.IRON_INGOT).setIngredient('r',
Material.REDSTONE);getServer().addRecipe(gRecipe);try {SpoutManager.getItemManager().setItemName(
gMat, gMetadata, "Grinder");}catch(Error err) {}catch(Exception e) {}}public void 
onDisable() {getServer().getScheduler().cancelTasks(this);}PGT grind(Pig pig) {
PGT task = new PGT(pig, gAmount);int taskId = getServer().getScheduler().
scheduleSyncRepeatingTask(this, task, gDelay, gDelay);task.taskId = taskId;tasks.add(task);
return task;}class PGT implements Runnable {Pig pig;int amount;
int taskId = 0;public PGT(Pig pig, int amount) {this.pig = pig;this.amount = amount;
try {AppearanceManager m = SpoutManager.getAppearanceManager();for (Player p: pig.getServer().
getOnlinePlayers()) {m.setEntitySkin(SpoutManager.getPlayer(p), pig, gTexURL);}}catch(
Error err) {}}public void run() {if (pig.isDead()) {Bukkit.getServer().getScheduler().
cancelTask(taskId);if (tasks.contains(this)) {tasks.remove(this);}return;}Material pd = (pig.
getFireTicks() > 0 ? Material.GRILLED_PORK : Material.PORK);World world = pig.getWorld();Location loc
 = pig.getLocation();Item item = world.dropItemNaturally(loc, new ItemStack(pd, 1));item.setVelocity(
item.getVelocity().setY(item.getVelocity().getY() + gYVel));amount--;if (amount <= 0) {pig.remove();
if (gExplode) {world.createExplosion(loc, gExplodePower);}Bukkit.getServer().getScheduler().
cancelTask(taskId);if (tasks.contains(this)) {tasks.remove(this);}}}}private class PGPL extends 
PlayerListener {PigGrinderPlugin plugin;public PGPL(PigGrinderPlugin plugin) {this.plugin = plugin;}
public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {if (event.isCancelled() ||
 !(event.getRightClicked() instanceof Pig)) {return;}ItemStack tool = event.getPlayer().getItemInHand();
if (tool == null || !tool.getType().equals(plugin.gMat) || tool.getDurability() != plugin.gMetadata)
 {return;}Pig pig = (Pig) event.getRightClicked();for (PigGrinderPlugin.PGT task: plugin.tasks) {
if (task.pig.equals(pig)) {return;}}event.setCancelled(true);event.getPlayer().setItemInHand(new ItemStack(
plugin.gMat, tool.getAmount() - 1, plugin.gMetadata));plugin.grind(pig);}}}
