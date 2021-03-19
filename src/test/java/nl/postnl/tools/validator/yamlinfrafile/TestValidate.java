package nl.postnl.tools.validator.yamlinfrafile;

import static org.junit.Assert.*;
import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestValidate {

	Validator validator;
	final Logger logger = LoggerFactory.getLogger(TestValidate.class);
	private boolean SkipDeployToTst = false;
	private boolean SkipDeployToAcc = false;

	public TestValidate() {
		validator = new Validator("src/main/resources/infrafiletocheck.yml", "src/main/resources/Dockerfiletocheck");
		try {
			SkipDeployToTst = Boolean.parseBoolean(System.getProperty("SkipDeployToTst"));
		} catch (Exception e) {
			logger.debug("Property SkipDeployToTst not found or incorrect defaulting to {}.", SkipDeployToTst);
		}
		try {
			SkipDeployToAcc = Boolean.parseBoolean(System.getProperty("SkipDeployToAcc"));
		} catch (Exception e) {
			logger.info("Property SkipDeployToAcc not found or incorrect defaulting to {}.", SkipDeployToAcc);
		}
	}

	@Test
	public void testRuleHasInterfacesElement() {
		boolean passed = validator.ruleHasInterfacesElement();
		if (!passed) {
			logger.info("The Interfaces Element is not set correctly in infra file.");
		}
		assertEquals(true, passed);
	}

	@Test
	public void testRuleHastaskImageElement() {
		boolean passed = validator.ruleHastaskImageElement();
		if (!passed) {
			logger.info("The Taskimage Element is not set correctly in infra file.");
		}
		assertEquals(true, passed);
	}

	@Test
	public void testRuleHasContextElement() {
		boolean passed = validator.ruleHasContextElement();
		if (!passed) {
			logger.info("The Context Element is not set correctly in infra file. Must contain one of the following: c2c, cbs, ls, mailnl, ecs, other, pd, pnp, tgn, generic");
		}
		assertEquals(true, passed);
	}

	@Test
	public void testRuleHasAppFlowNameElement() {
		boolean passed = validator.ruleHasAppFlowNameElement();
		if (!passed) {
			logger.info("The App Flow Name Element is not set correctly in infra file.");
		}
		assertEquals(true, passed);
	}

	@Test
	public void testRuleCheckIfInfraParametersExistInDocker() {
		boolean passed = validator.ruleCheckIfInfraParametersExistInDocker();
		if (!passed) {
			logger.info("The infrafile contains parameters that are not present in the Dockerfile.");
		}
		assertEquals(true, passed);
	}

	@Test
	public void testRuleHasScalingElement() {
		boolean passed = validator.ruleHasScalingElement();
		if (!passed) {
			logger.info("The Scaling Element is not set correctly in infra file.");
		}
		assertEquals(true, passed);
	}

	@Test
	public void testRulePrdElementsOnAcc() {
		if (!SkipDeployToAcc) {
			boolean passed = validator.ruleHasPrdElementsOnAcc();
			if (!passed) {
				logger.info("Not all prd parameters are available for acc in the infra file.");
			}
			assertEquals(true, passed);
		} else {
			logger.info("Skip deploy to acc is {} therefore testRulePrdElementsOnAcc was skipped.", SkipDeployToAcc);
		}
	}

	@Test
	public void testRuleInfraContainsAllEnvironments() {
		boolean passed = validator.ruleInfraContainsAllEnvironments(SkipDeployToTst, SkipDeployToAcc);
		if (!passed) {
			logger.info("Not all expected environments are declared in the infra file");
		}
		assertEquals(true, passed);
	}
	
	@Test
    public void testRuleDuplicateEnvEntries() {
        boolean passed = validator.ruleDuplicateEnvEntries(SkipDeployToTst, SkipDeployToAcc);
        if (!passed) {
            logger.info("Duplicate env entries found !!!");
        }
        assertEquals(true, passed);
    }
	
	@Test
	public void testRuleContextPathLoadBalancer() {
		boolean passed = validator.ruleContextPathLoadBalancer();
		if (!passed) {
			logger.info("Context path or loadbalancer was not configured correctly.");
		}
		assertEquals(true, passed);
	}
}
