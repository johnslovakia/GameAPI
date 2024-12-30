package cz.johnslovakia.gameapi.utils;


import com.cryptomorin.xseries.XMaterial;
import cz.johnslovakia.gameapi.GameAPI;
import org.apache.commons.lang.Validate;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.material.Wool;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Easily create itemstacks, without messing your hands.
 * <i>Note that if you do use this in one of your projects, leave this notice.</i>
 * <i>Please do credit me if you do use this in one of your projects.</i>
 * @author NonameSL
 */
public class ItemBuilder {
    private ItemStack is;
    /**
     * Create a new ItemBuilder from scratch.
     * @param m The material to create the ItemBuilder with.
     */
    public ItemBuilder(Material m){
        this(m, 1);
    }
    /**
     * Create a new ItemBuilder over an existing itemstack.
     * @param is The itemstack to create the ItemBuilder over.
     */
    public ItemBuilder(ItemStack is){
        this.is=new ItemStack(is);
    }
    /**
     * Create a new ItemBuilder from scratch.
     * @param m The material of the item.
     * @param amount The amount of the item.
     */
    public ItemBuilder(Material m, int amount){
        is= new ItemStack(m, amount);
    }
    /**
     * Create a new ItemBuilder from scratch.
     * @param is The itemstack to create the ItemBuilder over.
     * @param amount The amount of the item.
     */
    public ItemBuilder(ItemStack is, int amount){
        this.is = is;
        is.setAmount(amount);
    }
    /**
     * Create a new ItemBuilder from scratch.
     * @param m The material of the item.
     * @param amount The amount of the item.
     * @param durability The durability of the item.
     */
    public ItemBuilder(Material m, int amount, byte durability){
        is = new ItemStack(m, amount, durability);
    }
    /**
     * Clone the ItemBuilder into a new one.
     * @return The cloned instance.
     */
    public ItemBuilder clone(){
        return new ItemBuilder(is);
    }
    /**
     * Change the durability of the item.
     * @param damage The durability to set it to.
     */
    public ItemBuilder damageItem(int damage){
        GameAPI.getInstance().getVersionSupport().setItemDamage(is, damage);
        return this;
    }
    /**
     * Set the displayname of the item.
     * @param name The name to change it to.
     */
    public ItemBuilder setName(String name){
        ItemMeta im = is.getItemMeta();
        im.setDisplayName(StringUtils.colorizer(name));
        is.setItemMeta(im);
        return this;
    }


    public ItemBuilder setUnbreakable(){
        GameAPI.getInstance().getVersionSupport().setItemUnbreakable(is);
        return this;
    }

    public ItemBuilder setItemStack(ItemStack is){
        this.is = is;
        return this;
    }
    public ItemBuilder setCustomModelData(int data){
        ItemMeta im = is.getItemMeta();
        Validate.notNull(im);
        im.setCustomModelData(data);
        is.setItemMeta(im);
        return this;
    }
    public ItemBuilder setBasePotionType(PotionType potionType){
        PotionMeta im = (PotionMeta) is.getItemMeta();
        Validate.notNull(im);
        im.setBasePotionType(potionType);
        is.setItemMeta(im);
        return this;
    }

    /**
     * Add an unsafe enchantment.
     * @param ench The enchantment to add.
     * @param level The level to put the enchant on.
     */
    public ItemBuilder addUnsafeEnchantment(Enchantment ench, int level){
        is.addUnsafeEnchantment(ench, level);
        return this;
    }
    /**
     * Remove a certain enchant from the item.
     * @param ench The enchantment to remove
     */
    public ItemBuilder removeEnchantment(Enchantment ench){
        is.removeEnchantment(ench);
        return this;
    }
    /**
     * Set the skull owner for the item. Works on skulls only.
     * @param owner The name of the skull's owner.
     */
    public ItemBuilder setSkullOwner(String owner){
        try{
            SkullMeta im = (SkullMeta)is.getItemMeta();
            im.setOwner(owner);
            is.setItemMeta(im);
        }catch(ClassCastException expected){}
        return this;
    }
    /**
     * Add an enchant to the item.
     * @param ench The enchant to add
     * @param level The level
     */
    public ItemBuilder addEnchant(Enchantment ench, int level){
        ItemMeta im = is.getItemMeta();
        im.addEnchant(ench, level, true);
        is.setItemMeta(im);
        return this;
    }

    public ItemBuilder addStoragedEnchantment(Enchantment ench, int level){
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta)is.getItemMeta();
        meta.addStoredEnchant(ench, level, true);
        is.setItemMeta(meta);
        return this;
    }

    /**
     * Add multiple enchants at once.
     * @param enchantments The enchants to add.
     */
    public ItemBuilder addEnchantments(Map<Enchantment, Integer> enchantments){
        is.addEnchantments(enchantments);
        return this;
    }
    /**
     * Sets infinity durability on the item by setting the durability to Short.MAX_VALUE.
     */
    public ItemBuilder setInfinityDurability(){
        is.setDurability(Short.MAX_VALUE);
        return this;
    }
    /**
     * Re-sets the lore.
     * @param lore The lore to set it to.
     */
    public ItemBuilder setLore(String... lore){
        ItemMeta im = is.getItemMeta();

        List<String> finalLore = new ArrayList<>();

        for (String line : lore){
            if (line.contains("\n")){
                String[] arrSplit = line.split("\n");
                finalLore.addAll(Arrays.asList(arrSplit));
            }else if (line.contains("/newline/")){
                String[] arrSplit = line.split("/newline/");
                finalLore.addAll(Arrays.asList(arrSplit));
            }else{
                finalLore.add(line);
            }
        }

        im.setLore(finalLore);
        is.setItemMeta(im);
        return this;
    }
    /**
     * Re-sets the lore.
     * @param lore The lore to set it to.
     */
    public ItemBuilder setLore(List<String> lore) {
        List<String> finalLore = new ArrayList<>();

        for (String line : lore){
            line = StringUtils.colorizer(line);
            if (line.contains("\n")){
                String[] arrSplit = line.split("\n");
                finalLore.addAll(Arrays.asList(arrSplit));
            }else if (line.contains("/newline/")){
                String[] arrSplit = line.split("/newline/");
                finalLore.addAll(Arrays.asList(arrSplit));
            }else{
                finalLore.add(line);
            }
        }

        ItemMeta im = is.getItemMeta();
        im.setLore(finalLore);
        is.setItemMeta(im);

        return this;
    }
    /*
     * Remove a lore line.
     * @param lore The lore to remove.
     */
    public ItemBuilder removeLoreLine(String line){
        ItemMeta im = is.getItemMeta();
        List<String> lore = new ArrayList<>(im.getLore());
        if(!lore.contains(line))return this;
        lore.remove(line);
        im.setLore(lore);
        is.setItemMeta(im);
        return this;
    }

    public ItemBuilder addFlags(ItemFlag... itemFlag) {
        ItemMeta im = this.is.getItemMeta();
        assert im != null;
        im.addItemFlags(/*new ItemFlag[]{*/itemFlag/*}*/);
        this.is.setItemMeta(im);
        return this;
    }

    public ItemBuilder hideAllFlags(){
        addFlags(ItemFlag.HIDE_ATTRIBUTES);
        addFlags(ItemFlag.HIDE_DYE);
        addFlags(ItemFlag.HIDE_ARMOR_TRIM);
        addFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        addFlags(ItemFlag.HIDE_DESTROYS);
        addFlags(ItemFlag.HIDE_ENCHANTS);
        addFlags(ItemFlag.HIDE_UNBREAKABLE);
        addFlags(ItemFlag.HIDE_PLACED_ON);
        return this;
    }

    /**
     * Remove a lore line.
     * @param index The index of the lore line to remove.
     */
    public ItemBuilder removeLoreLine(int index){
        ItemMeta im = is.getItemMeta();
        List<String> lore = new ArrayList<>(im.getLore());
        if(index<0||index>lore.size())return this;
        lore.remove(index);
        im.setLore(lore);
        is.setItemMeta(im);
        return this;
    }
    /**
     * Add a lore line.
     * @param line The lore line to add.
     */
    public ItemBuilder addLoreLine(String line){
        line = StringUtils.colorizer(line);

        ItemMeta im = is.getItemMeta();
        List<String> lore = new ArrayList<>();
        if(im.hasLore())lore = new ArrayList<>(im.getLore());

        if (line.contains("\n")){
            String[] arrSplit = line.split("\n");
            for (int i=0; i < arrSplit.length; i++){
                lore.add(arrSplit[i]);
            }
        }else if (line.contains("/newline/")){
            String[] arrSplit = line.split("/newline/");
            for (int i=0; i < arrSplit.length; i++){
                lore.add(arrSplit[i]);
            }
        }else{
            lore.add(line);
        }
        im.setLore(lore);
        is.setItemMeta(im);
        return this;
    }

    public ItemBuilder removeLore(){
        ItemMeta im = is.getItemMeta();
        im.setLore(null);
        is.setItemMeta(im);
        return this;
    }

    /**
     * Add a lore line.
     * @param line The lore line to add.
     * @param pos The index of where to put it.
     */
    public ItemBuilder addLoreLine(String line, int pos){
        ItemMeta im = is.getItemMeta();
        List<String> lore = new ArrayList<>(im.getLore());
        lore.set(pos, line);
        im.setLore(lore);
        is.setItemMeta(im);
        return this;
    }
    /**
     * Sets the dye color on an item.
     * <b>* Notice that this doesn't check for item type, sets the literal data of the dyecolor as durability.</b>
     * @param color The color to put.
     */
    @SuppressWarnings("deprecation")
    public ItemBuilder setDyeColor(DyeColor color){
        this.is.setDurability(color.getDyeData());
        return this;
    }
    /**
     * Sets the dye color of a wool item. Works only on wool.
     * @deprecated As of version 1.2 changed to setDyeColor.
     * @see ItemBuilder@setDyeColor(DyeColor)
     * @param color The DyeColor to set the wool item to.
     */
    @Deprecated
    public ItemBuilder setWool(DyeColor color){
        if(!is.getType().equals(XMaterial.WHITE_WOOL.parseMaterial()))return this;

        Wool wool = new Wool();
        wool.setColor(color);
        wool.toItemStack().setItemMeta(is.getItemMeta());

        this.is = wool.toItemStack();
        return this;
    }

    /**
     * Sets the armor color of a leather armor piece. Works only on leather armor pieces.
     * @param color The color to set it to.
     */
    public ItemBuilder setLeatherArmorColor(Color color){
        try{
            LeatherArmorMeta im = (LeatherArmorMeta)is.getItemMeta();
            im.setColor(color);
            is.setItemMeta(im);
        }catch(ClassCastException expected){}
        return this;
    }
    /**
     * Retrieves the itemstack from the ItemBuilder.
     * @return The itemstack created/modified by the ItemBuilder instance.
     */
    public ItemStack toItemStack(){
        return is;
    }
}
