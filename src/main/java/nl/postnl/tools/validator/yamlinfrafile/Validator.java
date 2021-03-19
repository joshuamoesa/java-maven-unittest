package nl.postnl.tools.validator.yamlinfrafile;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Validator {
    final Logger logger = LoggerFactory.getLogger(Validator.class);
    private Map<String, Object> ymlMap;
    private ArrayList<String> envList = new ArrayList<String>();
    private String dockerFileContents;

    private String infraFilePath;
    private String dockerFilePath;

    public Validator(String infraFilePath, String dockerFilePath) {
        this.setInfraFilePath(infraFilePath);
        this.setDockerFilePath(dockerFilePath);
        this.parseInfraFile();
        this.readDockerFile();
    }

    private void parseInfraFile() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {

            ymlMap = mapper.readValue(new File(this.getInfraFilePath()), new TypeReference<Map<String, Object>>() {
            });

            for (Map.Entry<String, Object> entry : ymlMap.entrySet()) {
                if (entry.getKey().equals("tst") || entry.getKey().equals("acc") || entry.getKey().equals("prd")) {
                    envList.add(entry.getKey());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Find a value inside an arraylist
     */
    private boolean existsInArrayList(ArrayList<?> arrayList, String value) {
        boolean found = false;
        if (arrayList != null) {
            for (int counter = 0; counter < arrayList.size() && !found; counter++) {
                found = arrayList.get(counter).toString().contains(value);
            }
        }
        return found;
    }

    /*
     * return script part of specific env
     */
    private ArrayList<String> getArrayListForEnvScript(String env) {
        for (Map.Entry<String, Object> entry : ymlMap.entrySet()) {
            if (entry.getKey().equals(env)) {
                LinkedHashMap<String, Object> envMap = (LinkedHashMap<String, Object>) entry.getValue();
                ArrayList<String> arrayList = (ArrayList<String>) envMap.get("script");
                return arrayList;
            }
        }
        return null;
    }

    private boolean existInInfraFileScaling(ArrayList<String> parameters) {
        ArrayList<Boolean> rulePassed = new ArrayList<Boolean>();
        for (String env : envList) {
            if (!env.equals("tst")) {
                for (String parameter : parameters) {
                    boolean newcheck = existsInArrayList(getArrayListForEnvScript(env), parameter);
                    rulePassed.add(newcheck);
                    if (!newcheck) {
                        logger.info("The following parameter for {} cannot be found in the infra file: {}", env,
                                parameter);
                    }
                }
            }
        }
        return (!rulePassed.contains(Boolean.FALSE));

    }

    private boolean existInInfraFile(String parameter) {
        boolean rulePassed = true;
        for (String env : envList)
            rulePassed = (rulePassed && existsInArrayList(getArrayListForEnvScript(env), parameter));

        return rulePassed;
    }

    public boolean checkPrdParameterOnAcc(String parameter, ArrayList<String> accParameterList) {
        boolean passed = true;
        if (!accParameterList.toString().contains(parameter)) {
            logger.info("Missing prd parameter on acc: {}", parameter);
            passed = false;
        }
        return passed;
    }

    /*
     * Rule: 1 Description: Rule that an INTERFACES element must exist inside script
     * part of each environment
     */
    public boolean ruleHasInterfacesElement() {
        return existInInfraFile("export INTERFACES");
    }

    /*
     * Rule: 2 Description: Rule that an TASK_IMAGE element must exist inside script
     * part of each environment
     */
    public boolean ruleHastaskImageElement() {
        return existInInfraFile("export TASK_IMAGE");
    }

    /*
     * Rule: 3 Description: Rule that a CONTEXT element exists and has a specific value from the domain map inside script
     * part of each environment
     */
    public boolean ruleHasContextElement() {
    	
    	boolean rulePassed = false;
    	
    	ArrayList<String> contextList = new ArrayList<String>(
                Arrays.asList("export CONTEXT=c2c",
                		"export CONTEXT=cbs",
                		"export CONTEXT=ecs",
                		"export CONTEXT=ls",
                		"export CONTEXT=mailnl",
                		"export CONTEXT=other",
                		"export CONTEXT=pd",
                		"export CONTEXT=pnp", 
                		"export CONTEXT=tgn",
                		"export CONTEXT=generic"));    	
    	
	    for (int counter = 0; counter < contextList.size() && !rulePassed; counter++) {
	    	rulePassed =  existInInfraFile(contextList.get(counter));
	    }
        
        return rulePassed;
    }

    /*
     * Rule: 4 Description: Rule that an APP_FLOW_NAME must exist inside script part
     * of each environment
     */
    public boolean ruleHasAppFlowNameElement() {
        return existInInfraFile("export APP_FLOW_NAME");
    }

    /*
     * Rule: 5 Description: Rule that all Infra parameters referring to docker must
     * exist in Dockerfile part of each environment
     */
    public boolean ruleCheckIfInfraParametersExistInDocker() {
        boolean rulePassed = true;
        for (String env : envList) {
            rulePassed = (rulePassed && dockerContainsInfraParameter(getArrayListForEnvScript(env)));
        }
        return rulePassed;
    }

    /*
     * Rule: 6 Description: Rule that 4 scaling parameters must exist inside script
     * part of each environment
     */
    public boolean ruleHasScalingElement() {
        ArrayList<String> scalingList = new ArrayList<String>(
                Arrays.asList("export SERVICE_AUTO_SCALING", "export SERVICE_AUTO_SCALING_MIN_CAPACITY",
                        "export SERVICE_AUTO_SCALING_MAX_CAPACITY", "export SERVICE_AUTO_SCALING_METRIC"));
        return existInInfraFileScaling(scalingList);
    }

    /*
     * Rule: 7 Description: Rule all prd parameters must exist inside script part of
     * the acc environment, excluded are the "SERVICE_AUTO_SCALING" parameters
     */
    public boolean ruleHasPrdElementsOnAcc() {
        ArrayList<Boolean> rulePassed = new ArrayList<Boolean>();
        ArrayList<String> prdParameterList = getArrayListForEnvScript("prd");
        ArrayList<String> accParameterList = getArrayListForEnvScript("acc");
        if (accParameterList != null && prdParameterList != null) {
            for (String parameter : prdParameterList) {
                if (parameter.contains("export ")) {
                    if (parameter.contains("PRD")) {
                        rulePassed.add(checkPrdParameterOnAcc(
                                parameter.substring(parameter.indexOf("A"), parameter.indexOf("=")), accParameterList));
                    } else {
                        if (parameter.contains("SERVICE_AUTO_SCALING")) {
                            logger.info(
                                    "AUTO_SCALING property found on PROD, excluded from requirement to be present on ACC");
                        } else {
                            rulePassed.add(checkPrdParameterOnAcc(parameter.substring(0, parameter.indexOf("=")),
                                    accParameterList));
                        }
                    }
                } else {
                    rulePassed.add(checkPrdParameterOnAcc(parameter, accParameterList));
                }
            }
        } 
        else if (prdParameterList == null) {
            logger.info(
                    "Prd environement was not found in the infra yml, ruleHasPrdElementsOnAcc was skipped.");
            return true;
        }
        else {
            logger.info(
                    "Acc and Prd environement was not found in the infra yml, ruleHasPrdElementsOnAcc was skipped.");
        }
        return (!rulePassed.contains(Boolean.FALSE));
    }

    /*
     * Rule: 8 Description: Check if all 3 environments should have parameters in
     * the infra file
     */
    public boolean ruleInfraContainsAllEnvironments(boolean SkipDeployToTst, boolean SkipDeployToAcc) {
        ArrayList<String> requiredEnvList = new ArrayList<String>();
        // prd is not "required"
        boolean rulePassed = true;
        if (!SkipDeployToTst) {
            requiredEnvList.add("tst");
        }
        if (!SkipDeployToAcc) {
            requiredEnvList.add("acc");
        }
        for (String env : requiredEnvList)
            if (!envList.contains(env)) {
                logger.info("Required environment: {} is missing from the infra file.", env);
                rulePassed = false;
            }
        return rulePassed;
    }

    /*
     * Rule: 9 Description: Check if either load balancer or context path is
     * configured and if so if both are configured
     */
    public boolean ruleContextPathLoadBalancer() {
        boolean rulePassed = true;
        if (existInInfraFile("export TASK_LOAD_BALANCER=true") && !existInInfraFile("_CONTEXT_PATH")) {
            logger.info("Load balancer is configured but context path is missing.");
            rulePassed = false;
        } else if (existInInfraFile("_CONTEXT_PATH") && !existInInfraFile("export TASK_LOAD_BALANCER=true")) {
            logger.info("Context path is configured but load balancer is missing.");
            rulePassed = false;
        }
        return rulePassed;
    }

    private void readDockerFile() {
        StringBuilder sb = new StringBuilder("");
        try {
            File myObj = new File(this.getDockerFilePath());
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                sb.append(data);
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        this.setDockerFileContents(sb.toString());
    }

    
    /* 
     * Rule: 10 Description: Check for duplicate entries in env variables
     */
    public boolean ruleDuplicateEnvEntries(boolean SkipDeployToTst, boolean SkipDeployToAcc) {
        boolean isError = false;
        Set<String> dups = new HashSet<String>();
        Set<String> tmp1 = new HashSet<String>();
        List<String> envNames = new ArrayList<>(Arrays.asList("prd")); 
        if (!SkipDeployToTst) {
            envNames.add("tst");
        }
        if (!SkipDeployToAcc) {
            envNames.add("acc");
        }
        for (String env: envList)  {
            dups.clear();
            tmp1.clear();
            ArrayList<String> varList = getArrayListForEnvScript(env);
            for (String tstVar : varList) {
                tstVar = tstVar.split("=")[0];
                if (tstVar.toLowerCase().startsWith("export ") && !tmp1.add(tstVar)) {
                    dups.add(tstVar);
                }
            }
            if (dups.size() > 0) {
                isError = true;
                logger.info(String.format("Env: %s, duplicate entries: %s ", env, dups.toString()));
            }
        }
        return (isError == false);
    }    
    
    private boolean dockerContainsInfraParameter(ArrayList<String> arrayListForEnvScript) {
        boolean contains = true;
        if (arrayListForEnvScript != null) {
            for (String parameter : arrayListForEnvScript) {
                if (parameter.contains("export APP_ESB")) {
                    String dockerParameterNameFoundInInfra = parameter.substring(parameter.indexOf("E"),
                            parameter.indexOf("="));
                    if (!this.getDockerFileContents().contains(dockerParameterNameFoundInInfra)) {
                        logger.info("The following export in the infra file was not found Dockerfile for {}",
                                dockerParameterNameFoundInInfra);
                        contains = false;
                    }
                }
            }
        }
        return contains;
    }

    public String getInfraFilePath() {
        return infraFilePath;
    }

    public void setInfraFilePath(String infraFilePath) {
        this.infraFilePath = infraFilePath;
    }

    public String getDockerFilePath() {
        return dockerFilePath;
    }

    public void setDockerFilePath(String dockerFilePath) {
        this.dockerFilePath = dockerFilePath;
    }

    public String getDockerFileContents() {
        return dockerFileContents;
    }

    public void setDockerFileContents(String dockerFileContents) {
        this.dockerFileContents = dockerFileContents;
    }
}
