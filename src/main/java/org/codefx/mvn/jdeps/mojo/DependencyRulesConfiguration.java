package org.codefx.mvn.jdeps.mojo;

import org.codefx.mvn.jdeps.rules.ArrowRuleParser;
import org.codefx.mvn.jdeps.rules.DependencyJudge;
import org.codefx.mvn.jdeps.rules.DependencyJudgeBuilder;
import org.codefx.mvn.jdeps.rules.DependencyRule;
import org.codefx.mvn.jdeps.rules.MapDependencyJudge.MapDependencyJudgeBuilder;
import org.codefx.mvn.jdeps.rules.PackageInclusion;
import org.codefx.mvn.jdeps.rules.Severity;
import org.codefx.mvn.jdeps.rules.SimpleDependencyJudge;
import org.codefx.mvn.jdeps.rules.XmlRule;
import org.codehaus.plexus.classworlds.launcher.ConfigurationException;

import java.util.List;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.codefx.mvn.jdeps.mojo.MojoLogging.logger;

/**
 * Captures the MOJO configuration that pertains the dependency rules and {@link #createJudge() creates} the
 * according {@link DependencyJudge}.
 */
class DependencyRulesConfiguration {

	/*
	 * It looks like this class belongs into 'org.codefx.mvn.jdeps.rules' (look at the imports) but
	 * stupid logging is in the way. I'd rather have a class that, in spirit, belongs somewhere else hang around here
	 * than introduce Maven-specific logging into the other package.
	 */

	private final Severity defaultSeverity;
	private final PackageInclusion packageInclusion;
	private final List<XmlRule> xml;
	private final List<String> arrow;

	public DependencyRulesConfiguration(
			Severity defaultSeverity, PackageInclusion packageInclusion, List<XmlRule> xml, List<String> arrow) {
		this.defaultSeverity = requireNonNull(defaultSeverity, "The argument 'defaultSeverity' must not be null.");
		this.packageInclusion = requireNonNull(packageInclusion, "The argument 'packageInclusion' must not be null.");
		this.xml = requireNonNull(xml, "The argument 'xml' must not be null.");
		this.arrow = requireNonNull(arrow, "The argument 'arrow' must not be null.");
	}

	/**
	 * @return the {@link DependencyJudge} matching the configuration
	 */
	public DependencyJudge createJudge() throws ConfigurationException {
		if (xml.isEmpty() && arrow.isEmpty())
			return new SimpleDependencyJudge(defaultSeverity);

		DependencyJudgeBuilder dependencyJudgeBuilder = createBuilderFromConfiguration();
		addXmlRulesToBuilder(xml, dependencyJudgeBuilder);
		addArrowRulesToBuilder(arrow, dependencyJudgeBuilder);
		return dependencyJudgeBuilder.build();
	}

	private DependencyJudgeBuilder createBuilderFromConfiguration() {
		return new MapDependencyJudgeBuilder()
				.withInclusion(packageInclusion)
				.withDefaultSeverity(defaultSeverity);
	}

	static void addXmlRulesToBuilder(List<XmlRule> xmlRules, DependencyJudgeBuilder dependencyJudgeBuilder)
			throws ConfigurationException {
		logStartAddingRules(xmlRules, "XML");

		for (XmlRule rule : xmlRules) {
			DependencyRule dependencyRule = rule.asDependencyRule();
			dependencyJudgeBuilder.addDependency(dependencyRule);
			logAddedRule(dependencyRule);
		}

		logDoneAddingRules(xmlRules, "XML");
	}

	static void addArrowRulesToBuilder(List<String> arrowRules, DependencyJudgeBuilder dependencyJudgeBuilder)
			throws ConfigurationException {
		logStartAddingRules(arrowRules, "Arrow");

		// the loop can be no stream because 'ArrowRuleParser.parseRules' throws a checked exception
		for (String arrowRule : arrowRules) {
			ArrowRuleParser
					.parseRules(arrowRule)
					.forEach(rule -> {
						dependencyJudgeBuilder.addDependency(rule);
						logAddedRule(rule);
					});
		}

		logDoneAddingRules(arrowRules, "Arrow");
	}

	private static void logStartAddingRules(List<?> rules, String ruleName) {
		if (!rules.isEmpty())
			logger().debug("\t" + ruleName + " rules:");
	}

	private static void logAddedRule(DependencyRule dependencyRule) {
		logger().debug("\t\t" + dependencyRule);
	}

	private static void logDoneAddingRules(List<?> rules, String ruleName) {
		if (rules.isEmpty())
			logger().debug(format("\t%s rules: none configured", ruleName));
		else
			logger().debug(format("\ttotal: %d", rules.size()));
	}

}
