/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.logging.log4j.transform.gradle

import org.apache.logging.log4j.weaver.Constants
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.artifacts.dependencies.DefaultProjectDependency

class LoggingTransformPlugin implements Plugin<Project> {
	public static final String LOGGING_TRANSFORM = 'loggingTransform'
	public static final String COMPILE_JAVA = 'compileJava'
	public static final String CLASSES = 'classes'
	public static final String COMPILE_TEST_FIXTURES_JAVA = 'compileTestFixturesJava'

	@Override
	void apply(Project project) {
		LoggingTransformExtension extension = project.extensions.create(LOGGING_TRANSFORM, LoggingTransformExtension)
		registerTask(project, extension)
	}

	static void registerTask(Project project, LoggingTransformExtension extension) {
		project.tasks.register(LOGGING_TRANSFORM, Log4jWeaverTask) { Log4jWeaverTask task ->
			group = 'build'
			description = 'A task that provides logging transformation to add location information to your logs'
			task.dependsOn(COMPILE_JAVA)
			task.sourceDirectoryPath.set("${project.buildDir}/${extension.sourceDirectory}")
			task.outputDirectory.set(project.file("${project.buildDir}/${extension.outputDirectory}"))
			task.toleranceMillis.set(extension.toleranceMillis)
			task.includes.addAll(extension.includes)
			task.excludes.addAll(extension.excludes)
			task.excludes.add("**/*${Constants.LOCATION_CACHE_SUFFIX}.class")
			extension.dependencies.each { String dependency -> task.dependsOn(dependency)}
			if (extension.testFixturesEnabled) {
				project.tasks.getByName(COMPILE_TEST_FIXTURES_JAVA).dependsOn(task)
			}
			task.dependsOn(project.provider {
				project.configurations
						.collect { Configuration configuration -> configuration.dependencies }
						.flatten()
						?.findAll { Object dependency ->
							dependency in DefaultProjectDependency && dependency.name != project.name
						}
						?.collect { Object dependency -> dependency as DefaultProjectDependency }
						?.collect { DefaultProjectDependency dependency -> dependency.getDependencyProject() }
						?.collect { Project projectDep -> projectDep.tasks.getByName(CLASSES)}
			})
		}

		project.tasks.getByName(CLASSES).dependsOn(LOGGING_TRANSFORM)
	}
}
