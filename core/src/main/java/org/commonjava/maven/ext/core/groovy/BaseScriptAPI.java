/*
 * Copyright (C) 2012 Red Hat, Inc. (jcasey@redhat.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.maven.ext.core.groovy;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.ext.common.model.Project;
import org.commonjava.maven.ext.common.session.MavenSessionHandler;
import org.commonjava.maven.ext.io.ModelIO;

import java.io.File;
import java.util.List;
import java.util.Properties;

/**
 * Common API for developers wishing to implement groovy scripts for PME.
 */
public interface BaseScriptAPI
{
    /**
     * Return the current Project
     * @return a {@link Project} instance.
     */
    Project getProject();

    /**
    * Returns the entire collection of Projects
    * @return an {@link java.util.ArrayList} of {@link Project} instances.
    */
    List<Project> getProjects();

    /**
     * Obtain the GAV of the current project
     * @return a {@link ProjectVersionRef}
     */
    ProjectVersionRef getGAV();

    /**
     * Get the working directory (the execution root).
     * @return a {@link File} reference.
     */
    File getBaseDir();

    /**
     * Get the user properties
     * @return a {@link Properties} reference.
     */
    Properties getUserProperties();

    /**
     * Get the modelIO instance for remote artifact resolving.
     * @return a {@link ModelIO} reference.
     */
    ModelIO getModelIO();

    /**
     * Get the MavenSessionHandler instance
     * @return a {@link MavenSessionHandler} reference.
     */
    MavenSessionHandler getSession();

    /**
     * Get the current stage
     * @return a {@link InvocationStage} reference.
     */
    InvocationStage getInvocationStage();
}
