/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.server.deployment.module;

import org.jboss.as.server.ServerLogger;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;

/**
 * DUP thats adds dependencies that are availible to all deployments
 *
 * @author Stuart Douglas
 * @author Thomas.Diesler@jboss.com
 */
public class ServerDependenciesProcessor implements DeploymentUnitProcessor {

    private static ModuleIdentifier[] DEFAULT_MODULES = new ModuleIdentifier[] {
        ModuleIdentifier.create("sun.jdk"),
        ModuleIdentifier.create("ibm.jdk"),
        ModuleIdentifier.create("javax.api"),
        ModuleIdentifier.create("org.jboss.logging"),
        ModuleIdentifier.create("org.jboss.vfs"),
        ModuleIdentifier.create("org.apache.commons.logging"),
        ModuleIdentifier.create("org.apache.log4j"),
        ModuleIdentifier.create("org.slf4j"),
        ModuleIdentifier.create("org.jboss.logging.jul-to-slf4j-stub")
    };

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();
        for (ModuleIdentifier moduleId : DEFAULT_MODULES) {
            try {
                moduleLoader.loadModule(moduleId);
                boolean importServices = moduleId.getName().endsWith("jdk");
                moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, moduleId, false, false, importServices, false));
            } catch (ModuleLoadException ex) {
                ServerLogger.ROOT_LOGGER.debugf("Module not found: %s", moduleId);
            }
        }
    }

    @Override
    public void undeploy(final DeploymentUnit context) {
    }
}
