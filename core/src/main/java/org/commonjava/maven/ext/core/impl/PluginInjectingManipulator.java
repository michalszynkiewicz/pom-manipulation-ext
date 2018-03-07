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
package org.commonjava.maven.ext.core.impl;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.commonjava.maven.ext.common.ManipulationException;
import org.commonjava.maven.ext.common.model.Project;
import org.commonjava.maven.ext.core.ManipulationSession;
import org.commonjava.maven.ext.core.state.PluginInjectingState;
import org.commonjava.maven.ext.core.state.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.commonjava.maven.ext.core.util.IdUtils.ga;

/**
 * Simple manipulator that detects the presence of the <a href="https://github.com/jdcasey/project-sources-maven-plugin">project-sources-maven-plugin</a>,
 * and injects it into the base build if it's not present. This plugin will simply create a source archive for the project sources AFTER this extension
 * has run, but BEFORE any sources are altered or generated by the normal build process. Configuration consists of a couple of properties, documented
 * in {@link PluginInjectingState}.
 */
@Component( role = Manipulator.class, hint = "plugin-injection" )
public class PluginInjectingManipulator
    implements Manipulator
{
    private static final String PROJECT_SOURCES_GID = "org.commonjava.maven.plugins";

    private static final String PROJECT_SOURCES_AID = "project-sources-maven-plugin";

    private static final String PROJECT_SOURCES_COORD = ga( PROJECT_SOURCES_GID, PROJECT_SOURCES_AID );

    private static final String BMMP_GID = "com.redhat.rcm.maven.plugin";

    private static final String BMMP_AID = "buildmetadata-maven-plugin";

    private static final String BMMP_COORD = ga( BMMP_GID, BMMP_AID );

    private static final String BMMP_GOAL = "provide-buildmetadata";

    private static final String BMMP_EXEC_ID = "build-metadata";

    private static final String PROJECT_SOURCES_GOAL = "archive";

    private static final String PROJECT_SOURCES_EXEC_ID = "project-sources-archive";

    private static final String INITIALIZE_PHASE = "initialize";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private ManipulationSession session;

    @Override
    public void init( final ManipulationSession session )
        throws ManipulationException
    {
        this.session = session;
        session.setState( new PluginInjectingState( session.getUserProperties() ) );
    }

    /**
     * If enabled, grab the execution root pom (which will be the topmost POM in terms of directory structure). Check for the
     * presence of the project-sources-maven-plugin in the base build (/project/build/plugins/). Inject a new plugin execution for creating project
     * sources if this plugin has not already been declared in the base build section.
     */
    @Override
    public Set<Project> applyChanges( final List<Project> projects )
        throws ManipulationException
    {
        final PluginInjectingState state = session.getState( PluginInjectingState.class );

        // This manipulator will only run if its enabled *and* at least one other manipulator is enabled.
        if ( state.isEnabled() &&
             session.anyStateEnabled( State.activeByDefault ))
        {
            for ( final Project project : projects )
            {
                if ( project.isExecutionRoot() )
                {
                    logger.info( "Examining {} to apply sources/metadata plugins.", project );

                    final Model model = project.getModel();
                    Build build = model.getBuild();
                    if ( build == null )
                    {
                        build = new Build();
                        model.setBuild( build );
                    }

                    boolean changed = false;
                    final Map<String, Plugin> pluginMap = build.getPluginsAsMap();
                    if ( state.isProjectSourcesPluginEnabled() && !pluginMap.containsKey( PROJECT_SOURCES_COORD ) )
                    {
                        final PluginExecution execution = new PluginExecution();
                        execution.setId( PROJECT_SOURCES_EXEC_ID );
                        execution.setPhase( INITIALIZE_PHASE );
                        execution.setGoals( Collections.singletonList( PROJECT_SOURCES_GOAL ) );

                        final Plugin plugin = new Plugin();
                        plugin.setGroupId( PROJECT_SOURCES_GID );
                        plugin.setArtifactId( PROJECT_SOURCES_AID );
                        plugin.setVersion( state.getProjectSourcesPluginVersion() );
                        plugin.addExecution( execution );

                        build.addPlugin( plugin );

                        changed = true;
                    }

                    if ( state.isBuildMetadataPluginEnabled() && !pluginMap.containsKey( BMMP_COORD ) )
                    {
                        final PluginExecution execution = new PluginExecution();
                        execution.setId( BMMP_EXEC_ID );
                        execution.setPhase( INITIALIZE_PHASE );
                        execution.setGoals( Collections.singletonList( BMMP_GOAL ) );

                        final Xpp3Dom xml = new Xpp3Dom( "configuration" );

                        final Map<String, Object> config = new HashMap<>();
                        config.put( "createPropertiesReport", true );
                        config.put( "hideCommandLineInfo", false );
                        config.put( "hideJavaOptsInfo", false );
                        config.put( "activateOutputFileMapping", true );
                        config.put( "addJavaRuntimeInfo", true );

                        // Default name is build.properties but we currently prefer build.metadata.
                        config.put( "propertiesOutputFile", "build.metadata" );
                        // Deactivate features we don't want.
                        config.put( "createXmlReport", false );
                        config.put( "addLocallyModifiedTagToFullVersion", false );
                        config.put( "addToGeneratedSources", false );
                        config.put( "validateCheckout", false );
                        config.put( "forceNewProperties", true );
                        config.put( "addBuildDateToFullVersion", false );
                        config.put( "addHostInfo", false );
                        config.put( "addBuildDateInfo", false );
                        config.put( "addOsInfo", false );
                        config.put( "addMavenExecutionInfo", false );
                        config.put( "addToFilters", false);

                        final Xpp3Dom additionalLocations = new Xpp3Dom( "addToLocations" );
                        final Xpp3Dom additionalLocation = new Xpp3Dom( "addToLocation" );

                        xml.addChild( additionalLocations );
                        additionalLocations.addChild( additionalLocation );
                        additionalLocation.setValue( "${session.executionRootDirectory}" );

                        for ( final Map.Entry<String, Object> entry : config.entrySet() )
                        {
                            final Xpp3Dom child = new Xpp3Dom( entry.getKey() );
                            if ( entry.getValue() != null )
                            {
                                child.setValue( entry.getValue().toString() );
                            }

                            xml.addChild( child );
                        }

                        execution.setConfiguration( xml );

                        final Plugin plugin = new Plugin();
                        plugin.setGroupId( BMMP_GID );
                        plugin.setArtifactId( BMMP_AID );
                        plugin.setVersion( state.getBuildMetadataPluginVersion() );
                        plugin.addExecution( execution );

                        build.addPlugin( plugin );

                        changed = true;
                    }

                    if ( changed )
                    {
                        return Collections.singleton( project );
                    }
                }
            }
        }

        return Collections.emptySet();
    }

    @Override
    public int getExecutionIndex()
    {
        return 65;
    }

}
