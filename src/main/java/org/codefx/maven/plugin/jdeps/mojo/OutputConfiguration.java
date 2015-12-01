package org.codefx.maven.plugin.jdeps.mojo;

import org.codefx.maven.plugin.jdeps.result.MojoOutputStrategy;
import org.codefx.maven.plugin.jdeps.result.ResultOutputStrategy;
import org.codefx.maven.plugin.jdeps.result.RuleOutputFormat;
import org.codefx.maven.plugin.jdeps.result.RuleOutputStrategy;
import org.codefx.maven.plugin.jdeps.result.ViolationsToRuleTransformer;
import org.codefx.maven.plugin.jdeps.rules.DependencyRule;
import org.codefx.maven.plugin.jdeps.tool.LineWriter;
import org.codefx.maven.plugin.jdeps.tool.LineWriter.IfFileExists;
import org.codefx.maven.plugin.jdeps.tool.LineWriter.StaticContent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

class OutputConfiguration {

	private static final String DEFAULT_FILE_NAME = "dependency_rules.xml";
	private static final String DEFAULT_INDENT = "\t";

	private final boolean outputRules;
	private final RuleOutputFormat format;
	private final String filePath;

	public OutputConfiguration(
			boolean outputRules, RuleOutputFormat format, String filePath) {
		this.outputRules = requireNonNull(outputRules, "The argument 'outputRules' must not be null.");
		this.format = requireNonNull(format, "The argument 'format' must not be null.");
		this.filePath = requireNonNull(filePath, "The argument 'filePath' must not be null.");
	}

	public ResultOutputStrategy createOutputStrategy() {
		if (outputRules)
			return createRuleOutputStrategy();
		else
			return createMojoOutputStrategy();
	}

	private ResultOutputStrategy createRuleOutputStrategy() {
		StaticContent outputFormatStaticContent = format.getStaticContent(DEFAULT_INDENT);

		Function<DependencyRule, Stream<String>> toLinesTransformer =
				format.getToLinesTransformer(outputFormatStaticContent);
		LineWriter writer = new LineWriter(
				getFile(filePath), IfFileExists.APPEND_NEW_CONTENT, outputFormatStaticContent);

		return new RuleOutputStrategy(ViolationsToRuleTransformer::transform, toLinesTransformer, writer::write);
	}

	private static Path getFile(String path) {
		Path outputFile = Paths.get(path);
		if (Files.isDirectory(outputFile))
			return outputFile.resolve(DEFAULT_FILE_NAME);
		else
			return outputFile;
	}

	private ResultOutputStrategy createMojoOutputStrategy() {
		return new MojoOutputStrategy();
	}

}
