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

import org.apache.commons.io.FileUtils;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.ext.common.model.Project;
import org.commonjava.maven.ext.common.util.ProjectComparator;
import org.commonjava.maven.ext.common.util.WildcardMap;
import org.commonjava.maven.ext.core.ManipulationSession;
import org.commonjava.maven.ext.core.fixture.TestUtils;
import org.commonjava.maven.ext.core.state.CommonState;
import org.commonjava.maven.ext.core.state.DependencyState;
import org.commonjava.maven.ext.core.state.RelocationState;
import org.commonjava.maven.ext.core.state.VersioningState;
import org.commonjava.maven.ext.io.PomIO;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.rules.TemporaryFolder;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

public class ProjectComparatorTest
{
    private static final String RESOURCE_BASE = "properties/";

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder(  );

    @Rule
    public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();//.muteForSuccessfulTests();

    private WildcardMap<ProjectVersionRef> map = new WildcardMap<>();

    @Test
    public void testCompareNoChanges() throws Exception
    {
        ManipulationSession session = createUpdateSession();

        // Locate the PME project pom file. Use that to verify inheritance tracking.
        final File projectroot = new File (TestUtils.resolveFileResource( RESOURCE_BASE, "" )
                                          .getParentFile()
                                          .getParentFile()
                                          .getParentFile()
                                          .getParentFile(), "pom.xml" );
        PomIO pomIO = new PomIO();
        List<Project> projectOriginal = pomIO.parseProject( projectroot );
        List<Project> projectNew = pomIO.parseProject( projectroot );

        ProjectComparator.compareProjects( session,
                                           map,
                                           projectOriginal, projectNew );

        assertFalse( systemOutRule.getLog().contains( "-->" ) );
    }

    @Test
    public void testCompareChanges() throws Exception
    {
        ManipulationSession session = createUpdateSession();
        session.getUserProperties().put( RelocationState.DEPENDENCY_RELOCATIONS + "ch.qos.logback:@org.foobar.logback:", "" );
        RelocationState relocationState = new RelocationState( session.getUserProperties() );
        session.setState( relocationState );

        // Locate the PME project pom file. Use that to verify inheritance tracking.
        final File projectroot = new File (TestUtils.resolveFileResource( RESOURCE_BASE, "" )
                                                    .getParentFile()
                                                    .getParentFile()
                                                    .getParentFile()
                                                    .getParentFile(), "pom.xml" );
        PomIO pomIO = new PomIO();

        List<Project> projectOriginal = pomIO.parseProject( projectroot );
        List<Project> projectNew = pomIO.parseProject( projectroot );

        projectNew.forEach( project -> project.getModel().setVersion( project.getVersion() + "-redhat-1" ) );
        projectNew.forEach( project -> {
            if ( project.getModel().getDependencyManagement() != null )
            {
                project.getModel().getDependencyManagement().getDependencies().forEach( dependency -> dependency.setVersion( dependency.getVersion() + "-redhat-1" ) );
            }
        } );
        projectNew.forEach( project -> project.getModel().getDependencies().forEach( dependency -> {
            if ( dependency.getGroupId().equals( "ch.qos.logback" ))
            {
                dependency.setGroupId( "org.foobar.logback" );
            }
        } ) );

        ProjectComparator.compareProjects( session,
                                           relocationState.getDependencyRelocations(),
                                           projectOriginal, projectNew );


        assertTrue( systemOutRule.getLog().contains( "Managed dependencies :" ) );
        assertTrue( systemOutRule.getLog().contains( "Project version :" ) );
        assertTrue( systemOutRule.getLog().contains( "-redhat-1" ) );
        assertTrue( systemOutRule.getLog().contains( "-->" ) );
        assertTrue( systemOutRule.getLog().contains( "Unversioned relocation" ) );
        assertTrue( systemOutRule.getLog().contains( "org.foobar" ) );
        assertFalse( systemOutRule.getLog().contains( "Non-Aligned Managed dependencies" ) );
        assertFalse( systemOutRule.getLog().contains( "Non-Aligned Managed plugins" ) );
    }

    @Test
    public void testCompareChangesWithNonAligned() throws Exception
    {
        ManipulationSession session = createUpdateSession();
        session.getUserProperties().put( ProjectComparator.REPORT_NON_ALIGNED,  "true");
        RelocationState relocationState = new RelocationState( session.getUserProperties() );
        session.setState( relocationState );

        // Locate the PME project pom file. Use that to verify inheritance tracking.
        final File projectroot = new File (TestUtils.resolveFileResource( RESOURCE_BASE, "" )
                                                    .getParentFile()
                                                    .getParentFile()
                                                    .getParentFile()
                                                    .getParentFile(), "pom.xml" );
        PomIO pomIO = new PomIO();

        List<Project> projectOriginal = pomIO.parseProject( projectroot );
        List<Project> projectNew = pomIO.parseProject( projectroot );

        projectNew.forEach( project -> project.getModel().setVersion( project.getVersion() + "-redhat-1" ) );
        projectNew.forEach( project -> {
            if ( project.getModel().getDependencyManagement() != null )
            {
                project.getModel().getDependencyManagement().getDependencies().forEach( dependency -> {
                    if ( dependency.getGroupId().startsWith( "org" ) )
                    {
                        dependency.setVersion( dependency.getVersion() + "-redhat-1" );
                    }
                } );
            }
        } );

        ProjectComparator.compareProjects( session,
                                           relocationState.getDependencyRelocations(),
                                           projectOriginal, projectNew );


        assertTrue( systemOutRule.getLog().contains( "Managed dependencies :" ) );
        assertTrue( systemOutRule.getLog().contains( "Project version :" ) );
        assertTrue( systemOutRule.getLog().contains( "-redhat-1" ) );
        assertTrue( systemOutRule.getLog().contains( "-->" ) );
        assertFalse( systemOutRule.getLog().contains( "org.foobar" ) );
        assertTrue( systemOutRule.getLog().contains( "Non-Aligned Managed dependencies : com.fasterxml.jackson.core:jackson-annotations:jar:2." ) );
        assertTrue( systemOutRule.getLog().contains( "Non-Aligned Managed plugins : org.codehaus.mojo:animal-sniffer-maven-plugin:1.1" ) );
    }

    @Test
    public void testCompareChangesWithFile() throws Exception
    {
        File resultFile = temporaryFolder.newFile();

        ManipulationSession session = createUpdateSession();
        session.getUserProperties().put( RelocationState.DEPENDENCY_RELOCATIONS + "ch.qos.logback:@org.foobar.logback:", "" );
        session.getUserProperties().put( ProjectComparator.REPORT_OUTPUT_FILE,  resultFile.getAbsolutePath());

        RelocationState relocationState = new RelocationState( session.getUserProperties() );
        session.setState( relocationState );

        // Locate the PME project pom file. Use that to verify inheritance tracking.
        final File projectroot = new File (TestUtils.resolveFileResource( RESOURCE_BASE, "" )
                                                    .getParentFile()
                                                    .getParentFile()
                                                    .getParentFile()
                                                    .getParentFile(), "pom.xml" );
        PomIO pomIO = new PomIO();

        List<Project> projectOriginal = pomIO.parseProject( projectroot );
        List<Project> projectNew = pomIO.parseProject( projectroot );

        projectNew.forEach( project -> project.getModel().setVersion( project.getVersion() + "-redhat-1" ) );
        projectNew.forEach( project -> {
            if ( project.getModel().getDependencyManagement() != null )
            {
                project.getModel().getDependencyManagement().getDependencies().forEach( dependency -> dependency.setVersion( dependency.getVersion() + "-redhat-1" ) );
            }
        } );
        projectNew.forEach( project -> project.getModel().getDependencies().forEach( dependency -> {
            if ( dependency.getGroupId().equals( "ch.qos.logback" ))
            {
                dependency.setGroupId( "org.foobar.logback" );
            }
        } ) );

        ProjectComparator.compareProjects( session,
                                           relocationState.getDependencyRelocations(),
                                           projectOriginal, projectNew );


        assertTrue( systemOutRule.getLog().contains( "Managed dependencies :" ) );
        assertTrue( systemOutRule.getLog().contains( "Project version :" ) );
        assertTrue( systemOutRule.getLog().contains( "-redhat-1" ) );
        assertTrue( systemOutRule.getLog().contains( "-->" ) );
        assertTrue( systemOutRule.getLog().contains( "Unversioned relocation" ) );
        assertTrue( systemOutRule.getLog().contains( "org.foobar" ) );

        assertTrue( resultFile.exists() );
        String contents = FileUtils.readFileToString( resultFile, Charset.defaultCharset() );

        assertTrue( contents.contains( "Managed dependencies :" ) );
        assertTrue( contents.contains( "Project version :" ) );
        assertTrue( contents.contains( "-redhat-1" ) );
        assertTrue( contents.contains( "-->" ) );
        assertTrue( contents.contains( "Unversioned relocation" ) );
        assertTrue( contents.contains( "org.foobar" ) );
    }


    @Test
    public void messageFormatterTest()
    {
        FormattingTuple tuple = MessageFormatter.format( "this is a test {} and {}", "test", "foobar" );
        assertEquals( "this is a test test and foobar", tuple.getMessage() );
    }

    @SuppressWarnings( "deprecation" )
    private ManipulationSession createUpdateSession() throws Exception
    {
        ManipulationSession session = new ManipulationSession();

        final Properties p = new Properties();
        p.setProperty( "strictAlignment", "true" );
        p.setProperty( "strictViolationFails", "true" );
        p.setProperty( "version.suffix", "redhat-1" );
        p.setProperty( "scanActiveProfiles", "true" );
        session.setState( new DependencyState( p ) );
        session.setState( new VersioningState( p ) );
        session.setState( new CommonState( p ) );

        final MavenExecutionRequest req =
                        new DefaultMavenExecutionRequest().setUserProperties( p ).setRemoteRepositories( Collections.emptyList() );

        final PlexusContainer container = new DefaultPlexusContainer();
        final MavenSession mavenSession = new MavenSession( container, null, req, new DefaultMavenExecutionResult() );

        session.setMavenSession( mavenSession );

        return session;
    }
}
