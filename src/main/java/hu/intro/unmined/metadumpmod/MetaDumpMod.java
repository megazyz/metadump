/*
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015 Balázs Farkas <megasys@intro.hu>
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package hu.intro.unmined.metadumpmod;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;

import net.minecraftforge.common.BiomeDictionary;
import static net.minecraftforge.common.BiomeDictionary.Type;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.StatCollector;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLContainer;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.registry.FMLControlledNamespacedRegistry;
import net.minecraftforge.fml.common.registry.GameData;

import com.google.gson.stream.JsonWriter;

/**
 * @author megasys
 * 
 *         Dumps metadata information to JSON files at postInit()
 * 
 */
@Mod(modid = MetaDumpMod.MOD_ID, name = MetaDumpMod.MOD_NAME, version = MetaDumpMod.MOD_VERSION)
public class MetaDumpMod {

	public static final String MOD_ID = "MetaDump";
	public static final String MOD_NAME = "MetaDump";
	public static final String MOD_VERSION = "1.0.1";

	public static final String FILENAME_BIOMES = "metadump-biomes-%s.json";
	public static final String FILENAME_BLOCKS = "metadump-blocks-%s-%s.json";
	public static final String FILENAME_VERSION = "metadump-version-%s.json";

	private static final String JSON_INDENT = "  ";

	private void dumpBiomes() {
		try {
			JsonWriter writer = new JsonWriter(new OutputStreamWriter(
					new FileOutputStream(String.format(FILENAME_BIOMES,
							Loader.MC_VERSION)), "UTF-8"));

			writer.setIndent(JSON_INDENT);
			writer.beginObject();

			writer.name("Biomes");
			writer.beginArray();

			for (BiomeGenBase biome : BiomeGenBase.getBiomeGenArray()) {
				if (biome == null)
					continue;

				writer.beginObject();
				writeBiomeProperties(writer, biome);
				writer.endObject();
			}

			writer.endArray();

			writer.endObject();
			writer.close();

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void dumpBlocks() {
		try {
			FMLControlledNamespacedRegistry<Block> blockRegistry = GameData
					.getBlockRegistry();

			HashSet<String> mods = new HashSet<String>();

			for (int id = 0; id < 4096; id++) {
				Block block = blockRegistry.getObjectById(id);
				if (block != null) {
					String blockName = blockRegistry.getNameForObject(block)
							.toString();
					String modName = blockName.substring(0,
							blockName.indexOf(":"));
					mods.add(modName);
				}
			}

			for (String modName : mods) {

				String modNameAndVersion = getSafeModNameAndVersion(modName);
				JsonWriter writer = new JsonWriter(
						new OutputStreamWriter(new FileOutputStream(
								String.format(FILENAME_BLOCKS,
										Loader.MC_VERSION, modNameAndVersion)),
								"UTF-8"));

				writer.setIndent(JSON_INDENT);
				writer.beginObject();

				writer.name("Blocks");
				writer.beginArray();
				for (int id = 0; id < 4096; id++) {
					Block block = blockRegistry.getObjectById(id);
					if (block == null
							|| block.getUnlocalizedName().equals("tile.air"))
						continue;

					String blockName = blockRegistry.getNameForObject(block)
							.toString();
					if (!blockName.startsWith(modName + ":"))
						continue;

					writer.beginObject();

					writer.name("Id");
					writer.value(id);

					writer.name("Name");
					writer.value(blockName);

					writeBlockProperties(writer, block);

					writer.name("HasSubTypes");
					Item item = Item.getItemFromBlock(block);

					if (item != null && item.getHasSubtypes()) {
						writer.value(true);

						writeSubBlocks(writer, block, item);
					} else {
						writer.value(false);
					}

					writer.endObject();
				}
				writer.endArray();

				writer.endObject();
				writer.close();
			}

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void dumpVersionInfo() {
		try {
			JsonWriter writer = new JsonWriter(new OutputStreamWriter(
					new FileOutputStream(String.format(FILENAME_VERSION,
							Loader.MC_VERSION)), "UTF-8"));

			Loader loader = Loader.instance();

			writer.setIndent(JSON_INDENT);
			writer.beginObject();

			writer.name("MCVersionString");
			writer.value(loader.getMCVersionString());

			writer.name("MCPVersionString");
			writer.value(loader.getMCPVersionString());

			writer.name("FMLVersionString");
			writer.value(loader.getFMLVersionString());

			writer.name("ActiveModList");
			writeActiveModListArray(writer);

			writer.endObject();
			writer.close();

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String getSafeModNameAndVersion(String modName) {
		String escapedModName = modName.replaceAll("\\W+", "_");
		String modNameAndVersion = escapedModName;

		if (modName.equals("minecraft")) {
			modNameAndVersion += "-" + Loader.MC_VERSION;
		} else {
			for (ModContainer modContainer : Loader.instance()
					.getActiveModList()) {
				if (modContainer.getModId().toUpperCase().equals(modName.toUpperCase())) {
					modNameAndVersion += "-" + modContainer.getVersion();
					break;
				}
			}
		}
		return modNameAndVersion;
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		dumpVersionInfo();
		dumpBlocks();
		dumpBiomes();
	}

	private void writeActiveModListArray(JsonWriter writer) throws IOException {
		Loader loader = Loader.instance();
		writer.beginArray();
		for (ModContainer modContainer : loader.getActiveModList()) {
			writer.beginObject();

			writer.name("ModId");
			writer.value(modContainer.getModId());

			writer.name("Name");
			writer.value(modContainer.getName());

			writer.name("Version");
			writer.value(modContainer.getVersion());

			writer.name("DisplayVersion");
			writer.value(modContainer.getDisplayVersion());

			writer.name("Source");
			writer.value(modContainer.getSource().getName());

			writer.endObject();
		}
		writer.endArray();
	}

	private void writeBiomeProperties(JsonWriter writer, BiomeGenBase biome)
			throws IOException {
		writer.name("Id");
		writer.value(biome.biomeID);

		writer.name("Name");
		writer.value(biome.biomeName);

		Type[] types = BiomeDictionary.getTypesForBiome(biome);
		if (types != null) {
			writer.name("Type");
			writer.beginArray();
			for (int i = 0; i < types.length; i++) {
				writer.value(types[i].toString());
			}
			writer.endArray();
		}

		writer.name("Color");
		writer.value(String.format("#%06X", biome.color));

		writer.name("WaterColorMultiplier");
		writer.value(String.format("#%06x", biome.getWaterColorMultiplier()));

		writer.name("EnableSnow");
		writer.value(biome.getEnableSnow());

		writer.name("SpawningChance");
		writer.value(biome.getSpawningChance());

		writer.name("TempCategory");
		writer.value(biome.getTempCategory().toString());

		writer.name("Class");
		writeClassArray(writer, biome.getClass(), BiomeGenBase.class);
	}

	private void writeBlockMapColors(JsonWriter writer, Block block)
			throws IOException {
		String[] colors = new String[16];
		boolean isAllEqual = true;
		for (int i = 0; i < 16; i++) {
			IBlockState state = null;
			try {
				state = block.getStateFromMeta(i);
			} catch (Exception e) {
			}

			if (state != null) {
				colors[i] = String.format("#%06X",
						block.getMapColor(state).colorValue);

				if (i > 0 && isAllEqual && !colors[i].equals(colors[0]))
					isAllEqual = false;
			}
		}
		if (isAllEqual) {
			writer.name("MapColor");
			writer.value(colors[0]);
		} else {
			writer.name("MapColors");
			writer.beginArray();
			for (int i = 0; i < 16; i++) {
				writer.value(colors[i]);
			}
			writer.endArray();
		}
	}

	private void writeBlockProperties(JsonWriter writer, Block block)
			throws IOException {

		writer.name("UnlocalizedName");
		writer.value(StatCollector.translateToLocal(block.getUnlocalizedName()));

		writer.name("LocalizedName");
		writer.value(StatCollector.translateToLocal(block.getLocalizedName()));

		writer.name("CanProvidePower");
		writer.value(block.canProvidePower());

		writer.name("LightOpacity");
		writer.value(block.getLightOpacity());

		writer.name("LightValue");
		writer.value(block.getLightValue());

		writer.name("IsOpaqueCube");
		writer.value(block.isOpaqueCube());

		writer.name("IsFullBlock");
		writer.value(block.isFullBlock());

		writer.name("IsFullCube");
		writer.value(block.isFullCube());

		writer.name("IsNormalCube");
		writer.value(block.isNormalCube());

		writer.name("IsSolidFullCube");
		writer.value(block.isSolidFullCube());

		writer.name("IsOpaqueCube");
		writer.value(block.isOpaqueCube());

		writer.name("IsTranslucent");
		writer.value(block.isTranslucent());

		writer.name("IsVisuallyOpaque");
		writer.value(block.isVisuallyOpaque());

		writer.name("RenderType");
		writer.value(block.getRenderType());

		writer.name("IsCollidable");
		writer.value(block.isCollidable());

		writer.name("IsFlowerPot");
		writer.value(block.isFlowerPot());

		writer.name("UseNeighborBrightness");
		writer.value(block.getUseNeighborBrightness());

		writer.name("Material");
		writer.beginObject();
		writeMaterialProperties(writer, block.getMaterial());
		writer.endObject();

		writer.name("Class");
		writeClassArray(writer, block.getClass(), Block.class);

		writeBlockMapColors(writer, block);
	}

	private void writeClassArray(JsonWriter writer, Class value, Class root)
			throws IOException {
		writer.beginArray();
		while (value != null && value != root) {
			writer.value(value.getName());
			value = value.getSuperclass();
		}
		writer.endArray();
	}

	private void writeMaterialProperties(JsonWriter writer, Material material)
			throws IOException {
		writer.name("IsSolid");
		writer.value(material.isSolid());

		writer.name("IsLiquid");
		writer.value(material.isLiquid());

		writer.name("IsOpaque");
		writer.value(material.isOpaque());

		writer.name("CanBurn");
		writer.value(material.getCanBurn());
	}

	private void writeSubBlocks(JsonWriter writer, Block block, Item item)
			throws IOException {
		ArrayList<ItemStack> list = new ArrayList<ItemStack>();
		try {
			item.getSubItems(item, CreativeTabs.tabAllSearch, list);
		} catch (Exception e) {
			writer.name("SubBlocksException");
			writer.value(e.toString());
			return;
		}

		writer.name("SubBlocks");
		writer.beginArray();
		for (ItemStack i : list) {
			writer.beginObject();

			writer.name("ItemDamage");
			writer.value(i.getItemDamage());

			writer.name("DisplayName");
			writer.value(StatCollector.translateToLocal(i.getDisplayName()));

			writer.name("UnlocalizedName");
			writer.value(StatCollector.translateToLocal(i.getUnlocalizedName()));

			writer.endObject();
		}
		writer.endArray();
	}
}
