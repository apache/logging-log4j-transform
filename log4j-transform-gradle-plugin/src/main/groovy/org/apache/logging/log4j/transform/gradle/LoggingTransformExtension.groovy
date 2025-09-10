package org.apache.logging.log4j.transform.gradle

import org.apache.logging.log4j.weaver.Constants

class LoggingTransformExtension {
	String sourceDirectory = 'classes/java/main/'
	String outputDirectory = 'classes/java/main/'
	long toleranceMillis = 1000
	Set<String> includes = ['**/*.class']
	Set<String> excludes  = [
		'**/*' + Constants.LOCATION_CACHE_SUFFIX + '.class'
	]
	Set<String> dependencies = []
	boolean testFixturesEnabled = false
}
