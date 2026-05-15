package com.arcadesmasher.guiinspector.mappings;

import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.adapter.MappingNsRenamer;
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

// this code is really messy
public class MappingsResolver {

	private static final Logger LOGGER = LoggerFactory.getLogger("MappingsResolver");

	private static final String NS_OFFICIAL     = "official";
	private static final String NS_INTERMEDIARY = "intermediary";
	private static final String NS_NAMED        = "named";
	private static final String NS_MOJMAP       = "named";

	private static final String YARN_RESOURCE_PATH = "/mappings/yarn-mappings.jar";
	private static final String TINY_ENTRY_PATH    = "mappings/mappings.tiny";

	private static final String INTERMEDIARY_URL = "https://maven.fabricmc.net/net/fabricmc/intermediary/%s/intermediary-%s-v2.jar";
	private static final String VERSION_MANIFEST_URL = "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json";

	private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(30);

	private MappingTree yarnTree;
	private MappingTree mojmapTree;

	public void loadYarn() throws MappingsException {
//		LOGGER.info("Loading bundled Yarn mappings...");
		try {
			Path tempJar = Files.createTempFile("yarn-mappings", ".jar");
			tempJar.toFile().deleteOnExit();

			try (InputStream in = MappingsResolver.class.getResourceAsStream(YARN_RESOURCE_PATH)) {
				if (in == null) {
					throw new MappingsException("yarn-mappings.jar not found");
				}
				Files.copy(in, tempJar, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			}

			Path tinyFile = extractTinyFromJar(tempJar);

			MemoryMappingTree tree = new MemoryMappingTree();
			MappingReader.read(tinyFile, MappingFormat.TINY_2_FILE, tree);
			this.yarnTree = tree;
//			LOGGER.info("Yarn mappings loaded. Namespaces: {}", tree.getDstNamespaces());
		} catch (IOException e) {
			throw new MappingsException("Failed to load Yarn mappings", e);
		}
	}

	public void loadMojMap() throws MappingsException {
		String mcVersion = getMcVersion();
		Path cacheDir = getCacheDir(mcVersion);
		Path cachedTiny = cacheDir.resolve("mojmap-intermediary.tiny");

		try {
			Files.createDirectories(cacheDir);

			MemoryMappingTree composed;

			if (Files.exists(cachedTiny)) {
//				LOGGER.info("Loading MojMap mappings from cache: {}", cachedTiny);
				composed = new MemoryMappingTree();
				MappingReader.read(cachedTiny, MappingFormat.TINY_2_FILE, composed);
			} else {
//				LOGGER.info("Fetching MojMap mappings for Minecraft {}...", mcVersion);
				composed = fetchAndCompose(mcVersion, cacheDir, cachedTiny);
			}

			this.mojmapTree = composed;
//			LOGGER.info("MojMap mappings ready.");

		} catch (IOException | InterruptedException e) {
			throw new MappingsException("Failed to load MojMap mappings", e);
		}
	}

	public String getYarnClassName(String intermediaryName) {
		checkLoaded(yarnTree, "Yarn");
		return resolveClassName(yarnTree, intermediaryName, NS_INTERMEDIARY, NS_NAMED);
	}
	public String getMojMapClassName(String intermediaryName) {
		checkLoaded(mojmapTree, "MojMap");
		return resolveClassName(mojmapTree, intermediaryName, NS_INTERMEDIARY, NS_MOJMAP);
	}

	public String getYarnMethodName(Class<?> runtimeClass, String intermediaryMethodName) {
		checkLoaded(yarnTree, "Yarn");
		return resolveMethodWithInheritance(yarnTree, intermediaryMethodName, NS_INTERMEDIARY, NS_NAMED, runtimeClass);
	}
	public String getMojMapMethodName(Class<?> runtimeClass, String intermediaryMethodName) {
		checkLoaded(mojmapTree, "MojMap");
		return resolveMethodWithInheritance(mojmapTree, intermediaryMethodName, NS_INTERMEDIARY, NS_MOJMAP, runtimeClass);
	}

	public String getYarnFieldName(String intermediaryClassName, String intermediaryFieldName) {
		checkLoaded(yarnTree, "Yarn");
		return resolveFieldName(yarnTree, intermediaryClassName, intermediaryFieldName, NS_INTERMEDIARY, NS_NAMED);
	}
	public String getMojMapFieldName(String intermediaryClassName, String intermediaryFieldName) {
		checkLoaded(mojmapTree, "MojMap");
		return resolveFieldName(mojmapTree, intermediaryClassName, intermediaryFieldName, NS_INTERMEDIARY, NS_MOJMAP);
	}

	public boolean isYarnLoaded() { return yarnTree != null; }
	public boolean isMojMapLoaded() { return mojmapTree != null; }

	private static String resolveClassName(MappingTree tree, String srcName, String srcNs, String dstNs) {
		int srcNsId = tree.getNamespaceId(srcNs);
		int dstNsId = tree.getNamespaceId(dstNs);
		if (srcNsId == MappingTree.NULL_NAMESPACE_ID || dstNsId == MappingTree.NULL_NAMESPACE_ID) return null;

		// mapping-io uses '/' as separator internally, but Minecraft uses '.'
		String internalName = intermediaryToInternal(srcName);
		MappingTree.ClassMapping cls = tree.getClass(internalName, srcNsId);
		if (cls == null) return null;

		String result = cls.getName(dstNsId);
		return result != null ? internalToIntermediary(result) : null;
	}

	private String resolveMethodWithInheritance(MappingTree tree, String intermediaryMethodName, String srcNs, String dstNs, Class<?> runtimeClass) {
		if (runtimeClass == null || runtimeClass == Object.class) return intermediaryMethodName;

		String intermediaryClassName = runtimeClass.getName();
		String result = resolveMethodName(tree, intermediaryClassName, intermediaryMethodName, null, srcNs, dstNs);
		if (result != null) return result;

		// try enclosing classes
		Class<?> enclosing = runtimeClass.getEnclosingClass();
		while (enclosing != null) {
			result = resolveMethodName(tree, enclosing.getName(), intermediaryMethodName, null, srcNs, dstNs);
			if (result != null) return result + " (inner)";
			enclosing = enclosing.getEnclosingClass();
		}

		// walk superclass chain
		result = resolveMethodWithInheritance(tree, intermediaryMethodName, srcNs, dstNs, runtimeClass.getSuperclass());
		if (result != null && !result.equals(intermediaryMethodName)) return result;

		// walk interfaces too
		for (Class<?> iface : runtimeClass.getInterfaces()) {
			result = resolveMethodWithInheritance(tree, intermediaryMethodName, srcNs, dstNs, iface);
			if (result != null && !result.equals(intermediaryMethodName)) return result;
		}

		return intermediaryMethodName; // fallback to normal name
	}

	private static String resolveMethodName(MappingTree tree, String srcClassName, String srcMethodName, String descriptor, String srcNs, String dstNs) {
		int srcNsId = tree.getNamespaceId(srcNs);
		int dstNsId = tree.getNamespaceId(dstNs);
		if (srcNsId == MappingTree.NULL_NAMESPACE_ID || dstNsId == MappingTree.NULL_NAMESPACE_ID) return null;

		MappingTree.ClassMapping cls = tree.getClass(intermediaryToInternal(srcClassName), srcNsId);
		if (cls == null) return null;

		MappingTree.MethodMapping method = cls.getMethod(srcMethodName, descriptor, srcNsId);
		if (method == null) return null;

		return method.getName(dstNsId);
	}

	private static String resolveFieldName(MappingTree tree, String srcClassName, String srcFieldName, String srcNs, String dstNs) {
		int srcNsId = tree.getNamespaceId(srcNs);
		int dstNsId = tree.getNamespaceId(dstNs);
		if (srcNsId == MappingTree.NULL_NAMESPACE_ID || dstNsId == MappingTree.NULL_NAMESPACE_ID) return null;

		MappingTree.ClassMapping cls = tree.getClass(intermediaryToInternal(srcClassName), srcNsId);
		if (cls == null) return null;

		MappingTree.FieldMapping field = cls.getField(srcFieldName, null, srcNsId);
		if (field == null) return null;

		return field.getName(dstNsId);
	}

	private MemoryMappingTree fetchAndCompose(String mcVersion, Path cacheDir, Path cachedTiny)
			throws IOException, InterruptedException, MappingsException {

		HttpClient http = HttpClient.newBuilder().connectTimeout(HTTP_TIMEOUT).build();

		// fetch fabric intermediary jar (obf -> intermediary)
		Path intermediaryJar = cacheDir.resolve("intermediary.jar");
		if (!Files.exists(intermediaryJar)) {
			String url = String.format(INTERMEDIARY_URL, mcVersion, mcVersion);
//			LOGGER.info("Fetching intermediary mappings from {}", url);
			downloadTo(http, url, intermediaryJar);
		}

		// fetch mojang proguard mappings (mojmap -> obf)
		Path proguardFile = cacheDir.resolve("client.txt");
		if (!Files.exists(proguardFile)) {
			String proguardUrl = resolveMojangMappingsUrl(http, mcVersion);
//			LOGGER.info("Fetching Mojang mappings from {}", proguardUrl);
			downloadTo(http, proguardUrl, proguardFile);
		}

		// parse intermediary (obf -> intermediary)
		Path intermediaryTiny = extractTinyFromJar(intermediaryJar);
		MemoryMappingTree intermediaryTree = new MemoryMappingTree();
		MappingReader.read(intermediaryTiny, MappingFormat.TINY_2_FILE, intermediaryTree);
		// now namespaces are "official" (src=obf), "intermediary"

		// parse proguard (src="source"/MojMap, dst="target"/obf)
		MemoryMappingTree proguardTree = new MemoryMappingTree();
		MappingReader.read(proguardFile, MappingFormat.PROGUARD_FILE, proguardTree);
//		LOGGER.info("proguardTree src='{}', dst='{}'", proguardTree.getSrcNamespace(), proguardTree.getDstNamespaces());

		// flip so obf ("target") becomes source -> src="target", dst=["source"]
		MemoryMappingTree proguardFlippedRaw = new MemoryMappingTree();
		proguardTree.accept(new MappingSourceNsSwitch(proguardFlippedRaw, "target"));
//		LOGGER.info("proguardFlippedRaw src='{}', dst='{}'", proguardFlippedRaw.getSrcNamespace(), proguardFlippedRaw.getDstNamespaces());

		// rename "target" -> "official" and "source" -> "named" so namespaces align with intermediaryTree (src="official") and with NS_MOJMAP ("named")
		MemoryMappingTree proguardFlipped = new MemoryMappingTree();
		Map<String, String> nsRenames = new HashMap<>();
		nsRenames.put("target", NS_OFFICIAL);  // obf -> "official"
		nsRenames.put("source", NS_NAMED);     // mojmap names -> "named"
		proguardFlippedRaw.accept(new MappingNsRenamer(proguardFlipped, nsRenames));
//		LOGGER.info("proguardFlipped src='{}', dst='{}'", proguardFlipped.getSrcNamespace(), proguardFlipped.getDstNamespaces());
		// now src="official" (obf), dst=["named"] (mojmap) which matches intermediaryTree's src

		// compose into a single tree
		// intermediaryTree is already src="official", dst="intermediary"
		// proguardFlipped is src="official", dst="named"
		// merge intermediary into proguardFlipped as both share "official" for src
		MemoryMappingTree composedTree = new MemoryMappingTree();
		proguardFlipped.accept(composedTree);
		intermediaryTree.accept(composedTree);
		// composedTree: src="official", dst=["named", "intermediary"]

		return composedTree;
	}

	private String resolveMojangMappingsUrl(HttpClient http, String mcVersion)
			throws IOException, InterruptedException, MappingsException {

		String manifest = httpGetString(http, VERSION_MANIFEST_URL);

		// minimal JSON parsing, just find the version entry URL then fetch it for the mappings URL
		// indexOf to avoid pulling in a JSON library dependency
		// should probably use a real parser but whatever
		int versionIdx = manifest.indexOf("\"" + mcVersion + "\"");
		if (versionIdx == -1) {
			throw new MappingsException("Minecraft version " + mcVersion + " not found in version manifest");
		}

		int urlStart = manifest.indexOf("\"url\"", versionIdx) + 8; // skip `"url":"`
		int urlEnd   = manifest.indexOf("\"", urlStart);
		String versionJsonUrl = manifest.substring(urlStart, urlEnd);

		String versionJson = httpGetString(http, versionJsonUrl);

		// find client_mappings download URL
		int mappingsIdx = versionJson.indexOf("\"client_mappings\"");
		if (mappingsIdx == -1) {
			throw new MappingsException("client_mappings not found for version " + mcVersion +
					". This version may not have official mappings.");
		}
		int murlStart = versionJson.indexOf("\"url\"", mappingsIdx) + 8;
		int murlEnd   = versionJson.indexOf("\"", murlStart);
		return versionJson.substring(murlStart, murlEnd);
	}

	private static void downloadTo(HttpClient http, String url, Path dest)
			throws IOException, InterruptedException {
//		LOGGER.info("downloadTo URL = [{}]", url);
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.timeout(HTTP_TIMEOUT)
				.GET()
				.build();
		http.send(request, HttpResponse.BodyHandlers.ofFile(dest));
	}

	private static String httpGetString(HttpClient http, String url)
			throws IOException, InterruptedException {
//		LOGGER.info("httpGetString URL = [{}]", url);
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.timeout(HTTP_TIMEOUT)
				.GET()
				.build();
		return http.send(request, HttpResponse.BodyHandlers.ofString()).body();
	}

	private static Path extractTinyFromJar(Path jarPath) throws IOException, MappingsException {
		try (JarFile jar = new JarFile(jarPath.toFile())) {
			ZipEntry entry = jar.getEntry(TINY_ENTRY_PATH);
			if (entry == null) {
				throw new MappingsException("Expected entry '" + TINY_ENTRY_PATH + "' not found in " + jarPath);
			}
			Path temp = Files.createTempFile("mappings", ".tiny");
			temp.toFile().deleteOnExit();
			try (InputStream in = jar.getInputStream(entry)) {
				Files.copy(in, temp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			}
			return temp;
		}
	}

	private static Path getCacheDir(String mcVersion) {
		return net.fabricmc.loader.api.FabricLoader.getInstance()
				.getConfigDir()
				.resolve("mappings-resolver")
				.resolve(mcVersion);
	}

	private static String getMcVersion() {
		return MinecraftClient.getInstance().getGameVersion();
	}

	private static void checkLoaded(MappingTree tree, String name) {
		if (tree == null) {
			throw new IllegalStateException(name + " mappings have not been loaded. Call load" + name + "() first.");
		}
	}

	private static String intermediaryToInternal(String name) {
		return name.replace('.', '/');
	}
	private static String internalToIntermediary(String name) {
		return name.replace('/', '.');
	}

	public static class MappingsException extends Exception {
		public MappingsException(String message) { super(message); }
		public MappingsException(String message, Throwable cause) { super(message, cause); }
	}
}