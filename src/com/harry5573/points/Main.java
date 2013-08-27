/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.harry5573.points;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 *
 * @author Harry5573
 */
public class Main extends JavaPlugin implements Listener {

    private FileConfiguration users = null;
    private File usersfile = null;

    @Override
    public void onEnable() {
        System.out.println("[TnTPoints] Enabled");

        getServer().getPluginManager().registerEvents(this, this);

        try {
            loadUsers();
        } catch (IOException ex) {
            System.out.println("[TnTPoints] Error while loading users file");
        }
        saveDefaultConfig();

        addStaffRecipies();
    }

    @Override
    public void onDisable() {
        System.out.println("[TnTPoints] Disabled");

        getServer().clearRecipes();
    }

    public void loadUsers() throws IOException {
        if (usersfile == null) {
            usersfile = new File(getDataFolder(), "users.yml");
        }
        users = YamlConfiguration.loadConfiguration(usersfile);

        // Look for defaults in the jar
        InputStream defConfigStream = this.getResource("users.yml");
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            users.setDefaults(defConfig);
            users.save(usersfile);
        }
    }

    public FileConfiguration getUsers() {
        return users;
    }

    public void saveUsers() {
        if (users == null || usersfile == null) {
            return;
        }
        try {
            users.save(usersfile);
            System.out.println("[TnTPoints] Users saved!");
        } catch (IOException ex) {
            System.out.println("[TnTPoints] Error while saving users file");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent e) {
        if (!getUsers().contains("Users." + e.getPlayer().getName())) {
            System.out.println("[TnTPoints] No user data found for " + e.getPlayer().getName() + " creating...");
            getUsers().set("Users." + e.getPlayer().getName(), 0);
            saveUsers();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        Player p = null;
        if (sender instanceof Player) {
            p = (Player) sender;
        }
        if (command.getName().equalsIgnoreCase("tpoints")) {
            String prefix = getConfig().getString("prefix").replaceAll("(&([a-f0-9]))", "\u00A7$2");

            if (args.length == 0) {
                sender.sendMessage(prefix + " " + ChatColor.RED + "Usage: /tpoints <bal|addpoints|removepoints>");
                return true;
            }

            if (args.length == 1) {
                if (args[0].equalsIgnoreCase("bal") || (args[0].equalsIgnoreCase("balance"))) {
                    if (p == null) {
                        sender.sendMessage(prefix + " " + ChatColor.RED + "You must be a player to do that!");
                        return true;
                    }
                    sender.sendMessage(prefix + " " + ChatColor.DARK_PURPLE + "You currently have " + ChatColor.GOLD + getPoints(p) + ChatColor.DARK_PURPLE + " Points.");
                }
            }

            if (args.length == 3) {
                if (args[0].equals("addpoints")) {
                    if (!sender.isOp()) {
                        sender.sendMessage(prefix + " " + ChatColor.RED + "You do not have permission to do that");
                        return true;
                    }
                    Player pl = Bukkit.getPlayer(args[1]);
                    if (pl == null) {
                        sender.sendMessage(prefix + " " + ChatColor.LIGHT_PURPLE + "We could not find the player " + ChatColor.AQUA + pl);
                        return true;
                    }
                    try {
                        int addnumber = Integer.valueOf(args[2]);
                        addPoints(pl, addnumber);
                        sender.sendMessage(prefix + " " + ChatColor.AQUA + pl.getName() + ChatColor.GOLD + " now has " + ChatColor.AQUA + getPoints(pl) + ChatColor.GOLD + " points");
                        return true;
                    } catch (NumberFormatException e) {
                        sender.sendMessage(prefix + " " + ChatColor.GRAY + "That is not a number");
                        return true;
                    }
                }
                if (args[0].equals("removepoints")) {
                    if (!sender.isOp()) {
                        sender.sendMessage(prefix + " " + ChatColor.RED + "You do not have permission to do that");
                        return true;
                    }
                    Player pl = Bukkit.getPlayer(args[1]);
                    if (pl == null) {
                        sender.sendMessage(prefix + " " + ChatColor.LIGHT_PURPLE + "We could not find the player " + ChatColor.AQUA + args[1]);
                        return true;
                    }
                    try {
                        int removenumber = Integer.valueOf(args[2]);
                        removePoints(pl, removenumber);
                        sender.sendMessage(prefix + " " + ChatColor.AQUA + pl.getName() + ChatColor.GOLD + " now has " + ChatColor.AQUA + getPoints(pl) + ChatColor.GOLD + " points");
                        return true;
                    } catch (NumberFormatException e) {
                        sender.sendMessage(prefix + " " + ChatColor.GRAY + "That is not a number");
                        return true;
                    }
                } else {
                    sender.sendMessage(prefix + " " + ChatColor.RED + "Usage: /tpoints <add|removepoints> <player> <amount>");
                    return true;
                }
            }
        }
        return false;
    }

    public int getPoints(Player p) {
        int points = getUsers().getInt("Users." + p.getName());
        return points;
    }

    public void addPoints(Player p, int newpoints) {
        String prefix = getConfig().getString("prefix").replaceAll("(&([a-f0-9]))", "\u00A7$2");
        int oldpoints = getPoints(p);
        int newpoint = oldpoints + newpoints;
        getUsers().set("Users." + p.getName(), newpoint);
        saveUsers();
        p.sendMessage(prefix + " " + ChatColor.GOLD + "You now have " + ChatColor.AQUA + getPoints(p) + ChatColor.GOLD + " points");
    }

    public void removePoints(Player p, int newpoints) {
        String prefix = getConfig().getString("prefix").replaceAll("(&([a-f0-9]))", "\u00A7$2");
        int oldpoints = getPoints(p);
        int newpoint = oldpoints - newpoints;
        getUsers().set("Users." + p.getName(), newpoint);
        saveUsers();
        p.sendMessage(prefix + " " + ChatColor.GOLD + "You now have " + ChatColor.AQUA + getPoints(p) + ChatColor.GOLD + " points");
    }

    public void addStaffRecipies() {
        //Regen 2 staff
        ItemStack staffregen = new ItemStack(Material.STICK);
        staffregen.addUnsafeEnchantment(Enchantment.DURABILITY, 3);

        ItemMeta regenmeta = staffregen.getItemMeta();

        List regenlore = new ArrayList();
        regenlore.add(ChatColor.GREEN + "Uses left: 5");
        regenlore.add(ChatColor.BLUE + "Costs 1 TnTPoint Per Cast");

        regenmeta.setLore(regenlore);
        regenmeta.setDisplayName(ChatColor.LIGHT_PURPLE + "Potion Staff Of Regeneration");

        staffregen.setItemMeta(regenmeta);

        ShapelessRecipe regenrecipie = new ShapelessRecipe(staffregen).addIngredient(Material.STICK).addIngredient(Material.STICK).addIngredient(Material.GHAST_TEAR);
        getServer().addRecipe(regenrecipie);

        System.out.println("[TnTPoints] Added custom recipipes");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onStaffInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();

        List regenlore5 = new ArrayList();
        regenlore5.add(ChatColor.GREEN + "Uses left: 5");
        regenlore5.add(ChatColor.BLUE + "Costs 1 TnTPoint Per Cast");

        List regenlore4 = new ArrayList();
        regenlore4.add(ChatColor.GREEN + "Uses left: 4");
        regenlore4.add(ChatColor.BLUE + "Costs 1 TnTPoint Per Cast");

        List regenlore3 = new ArrayList();
        regenlore3.add(ChatColor.GREEN + "Uses left: 3");
        regenlore3.add(ChatColor.BLUE + "Costs 1 TnTPoint Per Cast");

        List regenlore2 = new ArrayList();
        regenlore2.add(ChatColor.GREEN + "Uses left: 2");
        regenlore2.add(ChatColor.BLUE + "Costs 1 TnTPoint Per Cast");

        List regenlore1 = new ArrayList();
        regenlore1.add(ChatColor.GREEN + "Uses left: 1");
        regenlore1.add(ChatColor.BLUE + "Costs 1 TnTPoint Per Cast");

        if (!e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        if (!p.isSneaking()) {
            return;
        }

        if (!p.getItemInHand().hasItemMeta()) {
            return;
        }

        if (!p.getItemInHand().getItemMeta().getDisplayName().equals(ChatColor.LIGHT_PURPLE + "Potion Staff Of Regeneration")) {
            return;
        }

        if (p.hasPotionEffect(PotionEffectType.REGENERATION)) {
            String prefix = getConfig().getString("prefix").replaceAll("(&([a-f0-9]))", "\u00A7$2");
            p.sendMessage(prefix + " " + ChatColor.GOLD + "You allready have regeneration applied");
            return;
        }

        if (getPoints(p) == 0) {
            String prefix = getConfig().getString("prefix").replaceAll("(&([a-f0-9]))", "\u00A7$2");
            p.sendMessage(prefix + " " + ChatColor.RED + "You do not have the needed 1 point to cast that staff");
            return;
        }
        removePoints(p, 1);


        if (p.getItemInHand().getItemMeta().getLore().equals(regenlore5)) {
            String prefix = getConfig().getString("prefix").replaceAll("(&([a-f0-9]))", "\u00A7$2");

            p.sendMessage(prefix + " " + ChatColor.GREEN + "Staff uses left: 4");
            regenFirework(p.getLocation(), p);
            p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 40, 1));
            ItemMeta newmeta = p.getItemInHand().getItemMeta();
            List regenlorelist4 = new ArrayList();
            regenlorelist4.clear();

            regenlorelist4.add(ChatColor.GREEN + "Uses left: 4");
            regenlorelist4.add(ChatColor.BLUE + "Costs 1 TnTPoint Per Cast");


            newmeta.setLore(regenlorelist4);
            p.getItemInHand().setItemMeta(newmeta);
            return;
        }

        if (p.getItemInHand().getItemMeta().getLore().equals(regenlore4)) {
            String prefix = getConfig().getString("prefix").replaceAll("(&([a-f0-9]))", "\u00A7$2");

            p.sendMessage(prefix + " " + ChatColor.GREEN + "Staff uses left: 3");
            regenFirework(p.getLocation(), p);
            p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 40, 1));
            ItemMeta newmeta1 = p.getItemInHand().getItemMeta();
            List regenlorelist3 = new ArrayList();
            regenlorelist3.clear();
            regenlorelist3.add(ChatColor.GREEN + "Uses left: 3");
            regenlorelist3.add(ChatColor.BLUE + "Costs 1 TnTPoint Per Cast");

            newmeta1.setLore(regenlorelist3);
            p.getItemInHand().setItemMeta(newmeta1);
            return;
        }

        if (p.getItemInHand().getItemMeta().getLore().equals(regenlore3)) {
            String prefix = getConfig().getString("prefix").replaceAll("(&([a-f0-9]))", "\u00A7$2");

            p.sendMessage(prefix + " " + ChatColor.GREEN + "Staff uses left: 2");
            regenFirework(p.getLocation(), p);
            p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 40, 1));
            ItemMeta newmeta2 = p.getItemInHand().getItemMeta();
            List regenlorelist2 = new ArrayList();
            regenlorelist2.clear();
            regenlorelist2.add(ChatColor.GREEN + "Uses left: 2");
            regenlorelist2.add(ChatColor.BLUE + "Costs 1 TnTPoint Per Cast");

            newmeta2.setLore(regenlorelist2);
            p.getItemInHand().setItemMeta(newmeta2);
            return;
        }

        if (p.getItemInHand().getItemMeta().getLore().equals(regenlore2)) {
            String prefix = getConfig().getString("prefix").replaceAll("(&([a-f0-9]))", "\u00A7$2");

            p.sendMessage(prefix + " " + ChatColor.GREEN + "Staff uses left: 1");
            regenFirework(p.getLocation(), p);
            p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 40, 1));
            ItemMeta newmeta3 = p.getItemInHand().getItemMeta();
            List regenlorelist1 = new ArrayList();
            regenlorelist1.clear();
            regenlorelist1.add(ChatColor.GREEN + "Uses left: 1");
            regenlorelist1.add(ChatColor.BLUE + "Costs 1 TnTPoint Per Cast");

            newmeta3.setLore(regenlorelist1);
            p.getItemInHand().setItemMeta(newmeta3);
            return;
        }

        //Final one before we break the staff
        if (p.getItemInHand().getItemMeta().getLore().equals(regenlore1)) {
            String prefix = getConfig().getString("prefix").replaceAll("(&([a-f0-9]))", "\u00A7$2");

            p.sendMessage(prefix + " " + ChatColor.GREEN + "Staff uses left: 0");
            regenFirework(p.getLocation(), p);
            p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 40, 1));
            p.getInventory().clear(p.getInventory().getHeldItemSlot());
            p.playSound(p.getLocation(), Sound.ITEM_BREAK, 5.0F, 5.0F);
            return;
        }
    }
    
    public void regenFirework(Location shootLocation, Player p) {
            Firework fw = p.getWorld().spawn(shootLocation, Firework.class);
            FireworkMeta fwm = fw.getFireworkMeta();
                     
            FireworkEffect effect = FireworkEffect.builder().withColor(Color.BLUE.mixColors(Color.YELLOW.mixColors(Color.GREEN))).with(Type.BALL_LARGE).withFade(Color.PURPLE).build();
 
 
                 
            fwm.addEffects(effect);
            fwm.setPower(1);     
            fw.setFireworkMeta(fwm);
    }
}
