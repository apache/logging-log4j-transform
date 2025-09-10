package org.apache.logging.log4j.transform.gradle

import org.apache.logging.log4j.weaver.Constants
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.apache.logging.log4j.weaver.LocationClassConverter
import org.apache.logging.log4j.weaver.LocationCacheGenerator
import org.codehaus.plexus.util.DirectoryScanner
import java.nio.file.Files
import java.nio.file.Path

/**
 * A Gradle task for weaving Log4j transformations into compiled class files using the log4j-weaver library.
 * This task mimics the functionality of the log4j-transform-maven-plugin's process-classes goal.
 */
abstract class Log4jWeaverTask extends DefaultTask {
	/**
	 * The directory containing the compiled source class files to process.
	 */
	@InputDirectory
	abstract Property<String> getSourceDirectoryPath()

	/**
	 * The directory where transformed class files will be written (typically the same as sourceDirectory for in-place transformation).
	 */
	@OutputDirectory
	abstract Property<File> getOutputDirectory()

	/**
	 * Tolerance in milliseconds for determining if a class file needs processing based on timestamps.
	 */
	@Input
	abstract Property<Long> getToleranceMillis()

	/**
	 * Set of include patterns for class files
	 */
	@Input
	abstract SetProperty<String> includes = project.objects.setProperty(String)

	/**
	 * Set of exclude patterns for class files.
	 */
	@Input
	abstract SetProperty<String> excludes = project.objects.setProperty(String)

	@Internal
	File sourceDirectory

	/**
	 * The main action of the task: weaves Log4j transformations into class files.
	 */
	@TaskAction
	void weave() {
		sourceDirectory = project.file(sourceDirectoryPath.get())
		logger.info("Starting Log4jWeaverTask: sourceDir=$sourceDirectory, outputDir=$outputDirectory, includes=$includes, excludes=$excludes")
		if (!sourceDirectory.exists()) {
			logger.warn("Skipping task: source directory ${sourceDirectory} does not exist")
			return
		}

		URLClassLoader classLoader = createClassLoader()
		LocationCacheGenerator locationCache = new LocationCacheGenerator()
		LocationClassConverter converter = new LocationClassConverter(classLoader)

		try {
			Set<Path> filesToProcess = getFilesToProcess(sourceDirectory.toPath(), outputDirectory.get().toPath())
			if (filesToProcess.empty) {
				logger.warn("No class files selected for transformation")
				return
			}

			filesToProcess.groupBy { Path path -> LocationCacheGenerator.getCacheClassFile(path) }
			.values()
			.each { List<Path> classFiles ->
				convertClassFiles(classFiles, converter, locationCache)
			}

			Map<String, byte[]> cacheClasses = locationCache.generateClasses()
			cacheClasses.each { String className, byte[] data ->
				saveCacheFile(className, data)
			}
		} catch (Exception e) {
			logger.error("Failed to process class files", e)
			throw new RuntimeException("Failed to process class files", e)
		}
	}

	/**
	 * Creates a ClassLoader including the source directory and runtime classpath dependencies.
	 *
	 * @return The created URLClassLoader.
	 */
	private URLClassLoader createClassLoader() {
		try {
			List<URL> urls = []
			urls << sourceDirectory.toURI().toURL()
			project.configurations.runtimeClasspath.files.each { File file ->
				urls << file.toURI().toURL()
			}
			URLClassLoader classLoader = new URLClassLoader(urls as URL[], Thread.currentThread().contextClassLoader)
			return classLoader
		} catch (Exception e) {
			logger.error("Failed to create ClassLoader", e)
			throw new RuntimeException("Failed to create ClassLoader", e)
		}
	}

	/**
	 * Scans the source directory for class files that need processing based on include/exclude patterns and timestamp checks.
	 *
	 * @param sourceDir The source directory path.
	 * @param outputDir The output directory path.
	 * @return Set of Path objects for class files that need processing.
	 */
	private Set<Path> getFilesToProcess(Path sourceDir, Path outputDir) {
		DirectoryScanner scanner = new DirectoryScanner()
		scanner.setBasedir(sourceDir.toFile())
		scanner.setIncludes(includes.get() as String[])
		scanner.setExcludes(excludes.get() as String[])
		scanner.scan()

		String[] includedFiles = scanner.getIncludedFiles()

		Set<Path> filesToProcess = includedFiles.findAll { String relativePath ->
			Path outputPath = outputDir.resolve(relativePath)
			return !Files.exists(outputPath) ||
					Files.getLastModifiedTime(sourceDir.resolve(relativePath)).toMillis() + toleranceMillis.get() >
					Files.getLastModifiedTime(outputPath).toMillis()
		}.collect { String relativePath ->
			sourceDir.resolve(relativePath)
		}.toSet()

		return filesToProcess
	}

	/**
	 * Converts a group of class files using the LocationClassConverter.
	 *
	 * @param classFiles List of class file paths to convert.
	 * @param converter The LocationClassConverter to use for transformation.
	 * @param locationCache The LocationCacheGenerator for cache management.
	 */
	protected void convertClassFiles(List<Path> classFiles, LocationClassConverter converter, LocationCacheGenerator locationCache) {
		Path sourceDir = sourceDirectory.toPath()
		ByteArrayOutputStream buf = new ByteArrayOutputStream()
		classFiles.sort()
		classFiles.each { Path classFile ->
			try {
				buf.reset()
				Files.newInputStream(sourceDir.resolve(classFile)).withCloseable { InputStream src ->
					converter.convert(src, buf, locationCache)
				}
				byte[] data = buf.toByteArray()
				saveClassFile(classFile, data)
			} catch (IOException e) {
				logger.error("Failed to process class file: ${sourceDir.relativize(classFile)}", e)
				throw new RuntimeException("Failed to process class file: ${sourceDir.relativize(classFile)}", e)
			}
		}
	}

	/**
	 * Saves a transformed class file to the output directory.
	 *
	 * @param dest The relative path of the class file to save.
	 * @param data The byte array of the transformed class file.
	 */
	protected void saveClassFile(Path dest, byte[] data) {
		Path outputPath = outputDirectory.get().toPath().resolve(dest)
		saveFile(outputPath, data)
		logger.info("Saved transformed class file: ${outputDirectory.get().toPath().relativize(outputPath)}")
	}

	/**
	 * Saves a generated cache class file to the output directory.
	 *
	 * @param internalClassName The internal name of the class (e.g., 'org/apache/logging/log4j/some/CacheClass').
	 * @param data The byte array of the cache class file.
	 */
	protected void saveCacheFile(String internalClassName, byte[] data) {
		Path outputPath = outputDirectory.get().toPath().resolve("${internalClassName}.class")
		saveFile(outputPath, data)
		logger.info("Saved cache class file: ${outputDirectory.get().toPath().relativize(outputPath)}")
	}

	protected static void saveFile(Path outputPath, byte[] data) {
		Files.createDirectories(outputPath.parent)
		Files.write(outputPath, data)
	}
}
