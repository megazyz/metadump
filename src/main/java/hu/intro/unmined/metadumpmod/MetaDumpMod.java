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

import net.minecraftforge.common.BiomeDictionary;
import static net.minecraftforge.common.BiomeDictionary.Type;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.StatCollector;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.common.MinecraftForge;

import com.google.gson.stream.JsonWriter;

import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.registry.FMLControlledNamespacedRegistry;
import cpw.mods.fml.common.registry.GameData;
import cpw.mods.fml.common.registry.GameData.GameDataSnapshot;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.ModContainer;

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
	public static final String MOD_VERSION = "1.0.0";

	public static final String FILENAME_BIOMES = "metadump.biomes.txt";
	public static final String FILENAME_BLOCKS = "metadump.blocks.txt";
	public static final String FILENAME_VERSION = "metadump.version.txt";

	private static final String JSON_INDENT = "  ";

	private void dumpBiomes() {
		try {
			JsonWriter writer = new JsonWriter(new OutputStreamWriter(
					new FileOutputStream(FILENAME_BIOMES), "UTF-8"));

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

			JsonWriter writer = new JsonWriter(new OutputStreamWriter(
					new FileOutputStream(FILENAME_BLOCKS), "UTF-8"));

			writer.setIndent(JSON_INDENT);
			writer.beginObject();

			writer.name("Blocks");
			writer.beginArray();
			for (int id = 0; id < 4096; id++) {
				Block block = blockRegistry.getObjectById(id);
				if (block == null
						|| block.getUnlocalizedName().equals("tile.air"))
					continue;

				writer.beginObject();

				writer.name("Id");
				writer.value(id);

				writer.name("Name");
				writer.value(blockRegistry.getNameForObject(block).toString());

				writeBlockProperties(writer, block);

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

	private void dumpVersionInfo() {
		try {
			JsonWriter writer = new JsonWriter(new OutputStreamWriter(
					new FileOutputStream(FILENAME_VERSION), "UTF-8"));

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
			colors[i] = String.format("#%06X", block.getMapColor(i).colorValue);

			if (i > 0 && isAllEqual && !colors[i].equals(colors[0]))
				isAllEqual = false;
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
}
