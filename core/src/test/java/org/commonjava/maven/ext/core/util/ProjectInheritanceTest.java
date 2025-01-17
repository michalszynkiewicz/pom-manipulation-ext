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

import org.apache.maven.model.Dependency;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;
import org.commonjava.maven.ext.common.model.Project;
import org.commonjava.maven.ext.core.ManipulationSession;
import org.commonjava.maven.ext.core.fixture.TestUtils;
import org.commonjava.maven.ext.io.PomIO;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemOutRule;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ProjectInheritanceTest
{
    private static final String RESOURCE_BASE = "properties/";

    @Rule
    public final SystemOutRule systemOutRule = new SystemOutRule().enableLog().muteForSuccessfulTests();

    @Test
    public void testVerifyInheritance() throws Exception
    {
        // Locate the PME project pom file. Use that to verify inheritance tracking.
        final File projectroot = new File (TestUtils.resolveFileResource( RESOURCE_BASE, "" )
                                          .getParentFile()
                                          .getParentFile()
                                          .getParentFile()
                                          .getParentFile(), "pom.xml" );
        PomIO pomIO = new PomIO();
        List<Project> projects = pomIO.parseProject( projectroot );
        for ( Project p : projects )
        {
            if ( ! p.getPom().equals( projectroot ) )
            {
                assertEquals( p.getProjectParent().getPom(), projectroot );
            }
        }
    }


    @Test
    public void testVerifyInheritanceMultiple() throws Exception
    {
        // Locate the PME project pom file. Use that to verify inheritance tracking.
        final File projectroot = new File (TestUtils.resolveFileResource( RESOURCE_BASE, "" )
                                                    .getParentFile()
                                                    .getParentFile()
                                                    .getParentFile()
                                                    .getParentFile(), "integration-test/src/it/project-inheritance/pom.xml" );
        PomIO pomIO = new PomIO();

        List<Project> projects = pomIO.parseProject( projectroot );

        for ( int i = 0; i<= 3 ; i++ )
        {
            if ( i == 0 )
            {
                // First project should be root
                assertNull( projects.get( i ).getProjectParent() );
                assertEquals( projects.get( i ).getPom(), projectroot );
                assertEquals( 1, projects.get( i ).getInheritedList().size() );
            }
            else if ( i == 1 )
            {
                List<Project> inherited = projects.get( i ).getInheritedList();
                assertEquals( 2, inherited.size() );
                assertEquals( inherited.get( 1 ).getProjectParent().getPom(), projectroot );
            }
            else if ( i == 2 )
            {
                List<Project> inherited = projects.get( i ).getInheritedList();
                assertEquals( 3, inherited.size() );
                assertEquals( inherited.get( 0 ).getPom(), projectroot );
            }
        }
    }

    @Test
    public void testVerifyExecutionRoot() throws Exception
    {
        // Locate the PME project pom file. Use that to verify inheritance tracking.
        final File projectroot = new File( TestUtils.resolveFileResource( RESOURCE_BASE, "" )
                                                    .getParentFile()
                                                    .getParentFile()
                                                    .getParentFile()
                                                    .getParentFile(),
                                           "integration-test/src/it/project-inheritance/common/pom.xml" );

        PomIO pomIO = new PomIO();

        List<Project> projects = pomIO.parseProject( projectroot );

        for ( Project p : projects )
        {
            if ( p.getKey().toString().equals( "io.apiman:apiman-common:1.2.7-SNAPSHOT" ))
            {
                assertTrue( p.isExecutionRoot() );
                assertTrue( p.isInheritanceRoot() );
            }
            else
            {
                assertFalse( p.isExecutionRoot() );
                assertFalse( p.isInheritanceRoot() );
            }
        }

    }

    @Test
    public void testVerifyRelativeExecutionRoot() throws Exception
    {
        final File projectRoot = new File( System.getProperty( "user.dir" ) + "/pom.xml" );

        Path root = Paths.get( projectRoot.getParent() );
        Path absolute = Paths.get( projectRoot.toString() );
        Path relative = root.relativize( absolute );

        PomIO pomIO = new PomIO();

        List<Project> projects = pomIO.parseProject( relative.toFile() );

        assertEquals( 1, projects.size() );
        assertTrue( projects.get( 0 ).isExecutionRoot() );
    }

    @Test
    public void testVerifyInheritanceReversedMultiple() throws Exception
    {
        // Locate the PME project pom file. Use that to verify inheritance tracking.
        final File projectroot = new File (TestUtils.resolveFileResource( RESOURCE_BASE, "" )
                                                    .getParentFile()
                                                    .getParentFile()
                                                    .getParentFile()
                                                    .getParentFile(), "integration-test/src/it/project-inheritance/pom.xml" );
        PomIO pomIO = new PomIO();

        List<Project> projects = pomIO.parseProject( projectroot );

        for ( int i = 0; i<= 3 ; i++ )
        {
            if ( i == 0 )
            {
                // First project should be root
                assertNull( projects.get( i ).getProjectParent() );
                assertEquals( projects.get( i ).getPom(), projectroot );
                assertEquals( 1, projects.get( i ).getReverseInheritedList().size() );
            }
            else if ( i == 1 )
            {
                List<Project> inherited = projects.get( i ).getReverseInheritedList();
                assertEquals( 2, inherited.size() );
                assertEquals( inherited.get( 0 ).getProjectParent().getPom(), projectroot );
            }
            else if ( i == 2 )
            {
                List<Project> inherited = projects.get( i ).getReverseInheritedList();
                assertEquals( 3, inherited.size() );
                assertEquals( inherited.get( 2 ).getPom(), projectroot );
            }
        }
    }



    @Test
    public void testVerifyProjectVersion() throws Exception
    {
        final ManipulationSession session = new ManipulationSession();

        final File projectroot = new File (TestUtils.resolveFileResource( RESOURCE_BASE, "" )
                                                    .getParentFile()
                                                    .getParentFile()
                                                    .getParentFile()
                                                    .getParentFile(), "integration-test/src/it/project-inheritance/pom.xml" );

        PomIO pomIO = new PomIO();
        List<Project> projects = pomIO.parseProject( projectroot );
        for ( Project p : projects )
        {
            if ( p.getPom().equals( projectroot ) )
            {
                Map<ArtifactRef, Dependency> deps = p.getResolvedManagedDependencies( session );
                for ( ArtifactRef a : deps.keySet())
                {
                    assertFalse ( a.getVersionString().contains( "project.version" ));
                }
                deps = p.getResolvedDependencies( session );
                assertEquals( 1, deps.size() );
                for ( ArtifactRef a : deps.keySet())
                {
                    assertFalse ( a.getVersionString().contains( "project.version" ));
                }
                assertFalse( deps.containsKey(  SimpleArtifactRef.parse( "org.mockito:mockito-all:jar:*" )));
                deps = p.getAllResolvedDependencies( session );
                assertEquals( 3, deps.size() );
                for ( ArtifactRef a : deps.keySet())
                {
                    assertFalse ( a.getVersionString().contains( "project.version" ));
                    if ( a.getGroupId().equals( "org.mockito" ) )
                    {
                        assertTrue ( a.getVersionString().contains( "*" ));
                    }
                }
                assertTrue( deps.containsKey( SimpleArtifactRef.parse( "org.mockito:mockito-all:jar:*" )));
            }
        }
    }

}
