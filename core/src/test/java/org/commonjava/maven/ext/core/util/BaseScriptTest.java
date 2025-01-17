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
package org.commonjava.maven.ext.core.util;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectRef;
import org.commonjava.maven.ext.common.model.Project;
import org.commonjava.maven.ext.core.ManipulationManager;
import org.commonjava.maven.ext.core.ManipulationSession;
import org.commonjava.maven.ext.core.fixture.TestUtils;
import org.commonjava.maven.ext.core.groovy.BaseScript;
import org.commonjava.maven.ext.core.impl.FinalGroovyManipulator;
import org.commonjava.maven.ext.core.impl.InitialGroovyManipulator;
import org.commonjava.maven.ext.core.state.VersioningState;
import org.commonjava.maven.ext.io.PomIO;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BaseScriptTest
{
    private static final String RESOURCE_BASE = "properties/";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Rule
    public final SystemOutRule systemRule = new SystemOutRule().enableLog().muteForSuccessfulTests();

    @Test
    public void testGroovyAnnotation() throws Exception
    {
        // Locate the PME project pom file. Use that to verify inheritance tracking.
        final File groovy = new File( TestUtils.resolveFileResource( RESOURCE_BASE, "" )
                                               .getParentFile()
                                               .getParentFile()
                                               .getParentFile()
                                               .getParentFile(), "integration-test/src/it/setup/depMgmt1/Sample.groovy" );
        final File projectroot = new File( TestUtils.resolveFileResource( RESOURCE_BASE, "" )
                                                    .getParentFile()
                                                    .getParentFile()
                                                    .getParentFile()
                                                    .getParentFile(), "pom.xml" );
        PomIO pomIO = new PomIO();
        List<Project> projects = pomIO.parseProject( projectroot );
        ManipulationManager m = new ManipulationManager( null, Collections.emptyMap(), Collections.emptyMap(), null );
        ManipulationSession ms = TestUtils.createSession( null );
        m.init( ms );

        Project root = projects.stream().filter( p -> p.getProjectParent() == null ).findAny().orElse( null );
        logger.info( "Found project root " + root );

        InitialGroovyManipulator gm = new InitialGroovyManipulator( null, null );
        gm.init( ms );
        TestUtils.executeMethod( gm, "applyGroovyScript", new Class[] { List.class, Project.class, File.class },
                                 new Object[] { projects, root, groovy } );
        assertTrue( systemRule.getLog().contains( "BASESCRIPT" ) );
    }

    @Test
    public void testGroovyAnnotationIgnore() throws Exception
    {
        // Locate the PME project pom file. Use that to verify inheritance tracking.
        final File groovy = new File( TestUtils.resolveFileResource( RESOURCE_BASE, "" )
                                               .getParentFile()
                                               .getParentFile()
                                               .getParentFile()
                                               .getParentFile(), "integration-test/src/it/setup/depMgmt1/Sample.groovy" );
        final File projectroot = new File( TestUtils.resolveFileResource( RESOURCE_BASE, "" )
                                                    .getParentFile()
                                                    .getParentFile()
                                                    .getParentFile()
                                                    .getParentFile(), "pom.xml" );

        PomIO pomIO = new PomIO();
        List<Project> projects = pomIO.parseProject( projectroot );
        ManipulationManager m = new ManipulationManager( null, Collections.emptyMap(), Collections.emptyMap(), null );
        ManipulationSession ms = TestUtils.createSession( null );
        m.init( ms );

        Project root = projects.stream().filter( p -> p.getProjectParent() == null ).findAny().orElse( null );
        logger.info( "Found project root " + root );

        FinalGroovyManipulator gm = new FinalGroovyManipulator( null, null );
        gm.init( ms );
        TestUtils.executeMethod( gm, "applyGroovyScript", new Class[] { List.class, Project.class, File.class },
                                 new Object[] { projects, root, groovy } );

        assertTrue( systemRule.getLog().contains( "Ignoring script" ) );
        assertFalse( systemRule.getLog().contains( "BASESCRIPT" ) );
    }

    @Test
    public void testInlineProperty() throws Exception
    {
        // Locate the PME project pom file. Use that to verify inheritance tracking.
        final File projectroot = new File( TestUtils.resolveFileResource( RESOURCE_BASE, "" )
                                                    .getParentFile()
                                                    .getParentFile()
                                                    .getParentFile()
                                                    .getParentFile(), "pom.xml" );
        PomIO pomIO = new PomIO();
        List<Project> projects = pomIO.parseProject( projectroot );
        ManipulationManager m = new ManipulationManager( null, Collections.emptyMap(), Collections.emptyMap(), null );
        ManipulationSession ms = TestUtils.createSession( null );
        m.init( ms );

        Project root = projects.stream().filter( p -> p.getProjectParent() == null ).findAny().orElseThrow(Exception::new);

        logger.info( "Found project root {}", root );

        BaseScript bs = new BaseScript()
        {
            @Override
            public Object run()
            {
                return null;
            }
        };
        bs.setValues( null, ms, projects, root, null );

        bs.inlineProperty( root, SimpleProjectRef.parse( "org.commonjava.maven.atlas:atlas-identities" ) );

        assertEquals( "0.17.1", root.getModel()
                                    .getDependencyManagement()
                                    .getDependencies()
                                    .stream()
                                    .filter( d -> d.getArtifactId().equals( "atlas-identities" ) )
                                    .findFirst()
                                    .orElseThrow(Exception::new)
                                    .getVersion() );

        bs.inlineProperty( root, "jacksonVersion" );

        assertFalse( root.getModel()
                         .getDependencyManagement()
                         .getDependencies()
                         .stream()
                         .filter( d -> d.getArtifactId().equals( "jackson-databind" ) )
                         .findFirst()
                         .orElseThrow(Exception::new)
                         .getVersion()
                         .contains( "$" ) );


    }

    @Test
    @SuppressWarnings( "deprecation" )
    public void testGroovyOverrideProperties() throws Exception
    {
        // Locate the PME project pom file. Use that to verify inheritance tracking.
        final File groovy = TestUtils.resolveFileResource( "", "PropertyOverride.groovy" );
        final File projectroot = new File( TestUtils.resolveFileResource( RESOURCE_BASE, "" )
                                                    .getParentFile()
                                                    .getParentFile()
                                                    .getParentFile()
                                                    .getParentFile(), "pom.xml" );
        final DefaultContainerConfiguration config = new DefaultContainerConfiguration();
        config.setClassPathScanning( PlexusConstants.SCANNING_ON );
        config.setComponentVisibility( PlexusConstants.GLOBAL_VISIBILITY );
        config.setName( "PME-CLI" );
        PlexusContainer container = new DefaultPlexusContainer( config);

        PomIO pomIO = container.lookup( PomIO.class );
        List<Project> projects = pomIO.parseProject( projectroot );

        Properties userProperties = new Properties(  );
        userProperties.setProperty( "versionIncrementalSuffix", "rebuild" );

        ManipulationManager manipulationManager = container.lookup( ManipulationManager.class );
        ManipulationSession session = container.lookup( ManipulationSession.class );
        MavenExecutionRequest req = new DefaultMavenExecutionRequest().setSystemProperties( System.getProperties() )
                                                                      .setUserProperties( userProperties )
                                                                      .setRemoteRepositories( Collections.emptyList() );
        MavenSession mavenSession = new MavenSession( container, null, req, new DefaultMavenExecutionResult() );
        session.setMavenSession( mavenSession );
        manipulationManager.init( session );


        VersioningState state = session.getState( VersioningState.class );
        assertNotNull( state );
        assertTrue( state.isEnabled() );
        assertEquals( "rebuild", state.getIncrementalSerialSuffix() );
        assertNull( state.getSuffix() );


        Project root = projects.stream().filter( p -> p.getProjectParent() == null ).findAny().orElse( null );
        logger.info( "Found project root " + root );

        InitialGroovyManipulator gm = new InitialGroovyManipulator( null, null );
        gm.init( session );
        TestUtils.executeMethod( gm, "applyGroovyScript", new Class[] { List.class, Project.class, File.class },
                                 new Object[] { projects, root, groovy } );

        assertTrue( systemRule.getLog().contains( "BASESCRIPT" ) );
        assertTrue( systemRule.getLog().contains( "STAGE FIRST" ) );
        assertTrue( systemRule.getLog().contains( "MODELIO null" ) );
        assertEquals( "rebuild-5", state.getSuffix() );
    }
}
