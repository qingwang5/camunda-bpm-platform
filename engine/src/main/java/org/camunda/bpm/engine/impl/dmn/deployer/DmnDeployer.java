/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.bpm.engine.impl.dmn.deployer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.camunda.bpm.dmn.engine.DmnDecision;
import org.camunda.bpm.dmn.engine.impl.spi.transform.DmnTransformer;
import org.camunda.bpm.engine.impl.AbstractDefinitionDeployer;
import org.camunda.bpm.engine.impl.ProcessEngineLogger;
import org.camunda.bpm.engine.impl.core.model.Properties;
import org.camunda.bpm.engine.impl.dmn.DecisionLogger;
import org.camunda.bpm.engine.impl.dmn.entity.repository.DecisionDefinitionEntity;
import org.camunda.bpm.engine.impl.dmn.entity.repository.DecisionDefinitionManager;
import org.camunda.bpm.engine.impl.dmn.entity.repository.DecisionRequirementDefinitionEntity;
import org.camunda.bpm.engine.impl.persistence.deploy.Deployer;
import org.camunda.bpm.engine.impl.persistence.deploy.DeploymentCache;
import org.camunda.bpm.engine.impl.persistence.entity.DeploymentEntity;
import org.camunda.bpm.engine.impl.persistence.entity.ResourceEntity;

/**
 * {@link Deployer} responsible to parse DMN 1.1 XML files and create the proper
 * {@link DecisionDefinitionEntity}s. Since it uses the result of the
 * {@link DrdDeployer} to avoid duplicated parsing, the DrdDeployer must
 * process the deployment before this deployer.
 */
public class DmnDeployer extends AbstractDefinitionDeployer<DecisionDefinitionEntity> {

  protected static final DecisionLogger LOG = ProcessEngineLogger.DECISION_LOGGER;

  public static final String[] DMN_RESOURCE_SUFFIXES = new String[] { "dmn11.xml", "dmn" };

  protected DmnTransformer transformer;

  @Override
  protected String[] getResourcesSuffixes() {
    return DMN_RESOURCE_SUFFIXES;
  }

  @Override
  protected List<DecisionDefinitionEntity> transformDefinitions(DeploymentEntity deployment, ResourceEntity resource, Properties properties) {
    List<DecisionDefinitionEntity> decisions = new ArrayList<DecisionDefinitionEntity>();

    // get the decisions from the deployed drd instead of parse the DMN again
    DecisionRequirementDefinitionEntity deployedDrd = findDeployedDrdForResource(deployment, resource.getName());

    if (deployedDrd == null) {
      throw LOG.exceptionNoDrdForResource(resource.getName());
    }

    Collection<DmnDecision> decisionsOfDrd = deployedDrd.getDecisions();
    for (DmnDecision decisionOfDrd : decisionsOfDrd) {

      DecisionDefinitionEntity decisionEntity = (DecisionDefinitionEntity) decisionOfDrd;
      if (DrdDeployer.isDecisionRequirementDefinitionPersistable(deployedDrd)) {
        decisionEntity.setDecisionRequirementDefinitionId(deployedDrd.getId());
      }

      decisions.add(decisionEntity);
    }

    return decisions;
  }

  protected DecisionRequirementDefinitionEntity findDeployedDrdForResource(DeploymentEntity deployment, String resourceName) {
    List<DecisionRequirementDefinitionEntity> deployedDrds = deployment.getDeployedArtifacts(DecisionRequirementDefinitionEntity.class);
    if (deployedDrds != null) {

      for (DecisionRequirementDefinitionEntity deployedDrd : deployedDrds) {
        if (deployedDrd.getResourceName().equals(resourceName)) {
          return deployedDrd;
        }
      }
    }
    return null;
  }

  @Override
  protected DecisionDefinitionEntity findDefinitionByDeploymentAndKey(String deploymentId, String definitionKey) {
    return getDecisionDefinitionManager().findDecisionDefinitionByDeploymentAndKey(deploymentId, definitionKey);
  }

  @Override
  protected DecisionDefinitionEntity findLatestDefinitionByKeyAndTenantId(String definitionKey, String tenantId) {
    return getDecisionDefinitionManager().findLatestDecisionDefinitionByKeyAndTenantId(definitionKey, tenantId);
  }

  @Override
  protected void persistDefinition(DecisionDefinitionEntity definition) {
    getDecisionDefinitionManager().insertDecisionDefinition(definition);
  }

  @Override
  protected void addDefinitionToDeploymentCache(DeploymentCache deploymentCache, DecisionDefinitionEntity definition) {
    deploymentCache.addDecisionDefinition(definition);
  }

  // context ///////////////////////////////////////////////////////////////////////////////////////////

  protected DecisionDefinitionManager getDecisionDefinitionManager() {
    return getCommandContext().getDecisionDefinitionManager();
  }

  // getters/setters ///////////////////////////////////////////////////////////////////////////////////

  public DmnTransformer getTransformer() {
    return transformer;
  }

  public void setTransformer(DmnTransformer transformer) {
    this.transformer = transformer;
  }

}
