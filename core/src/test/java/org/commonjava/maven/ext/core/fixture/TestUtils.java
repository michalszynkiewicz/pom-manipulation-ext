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
package org.commonjava.maven.ext.core.fixture;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.commonjava.maven.ext.common.ManipulationException;
import org.commonjava.maven.ext.core.ManipulationSession;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Properties;

public class TestUtils
{
    public static Model resolveModelResource( final String resourceBase, final String resourceName )
        throws Exception
    {
        return new MavenXpp3Reader().read( new FileReader( resolveFileResource( resourceBase, resourceName ) ) );
    }

    public static File resolveFileResource( final String resourceBase, final String resourceName )
                    throws Exception
    {
        final URL resource = Thread.currentThread()
                                   .getContextClassLoader()
                                   .getResource( resourceBase + resourceName );

        if ( resource == null )
        {
            throw new ManipulationException( "Unable to locate resource for " + resourceBase + resourceName );
        }
        return new File( resource.getPath() );
    }

    public static Model getDummyModel ()
    {
        Model result = new Model();
        result.setGroupId( "org.commonjava.maven.ext" );
        result.setArtifactId( "dummy-model" );
        result.setVersion( "1.0.0-SNAPSHOT" );
        return result;
    }

    @SuppressWarnings( "deprecation" )
    public static ManipulationSession createSession( Properties p ) throws ManipulationException
    {
        ManipulationSession session = new ManipulationSession();

        final MavenExecutionRequest req = new DefaultMavenExecutionRequest().setUserProperties( p );
        final PlexusContainer container;
        try
        {
            container = new DefaultPlexusContainer();
        }
        catch ( PlexusContainerException e )
        {
            throw new ManipulationException( "Unable to create DefaultPlexusContainer", e );
        }
        final MavenSession mavenSession = new MavenSession( container, null, req, new DefaultMavenExecutionResult() );

        session.setMavenSession( mavenSession );

        return session;
    }

    /**
     * Executes a method on an object instance.  The name and parameters of
     * the method are specified.  The method will be executed and the value
     * of it returned, even if the method would have private or protected access.
     */
    @SuppressWarnings( "unchecked" )
    public static Object executeMethod( Object instance, String name, Class[] types, Object[] params ) throws Exception
    {
        Class c = instance.getClass();

        Method m;
        try
        {
            m = c.getDeclaredMethod( name, types );
        }
        catch ( NoSuchMethodException e)
        {
            c = c.getSuperclass();
            m = c.getDeclaredMethod( name, types );
        }

        m.setAccessible( true );

        return m.invoke( instance, params );
    }

    /**
     * Executes a method on an object instance.  The name and parameters of
     * the method are specified.  The method will be executed and the value
     * of it returned, even if the method would have private or protected access.
     */
    @SuppressWarnings( "unchecked" )
    public static Object executeMethod( Object instance, String name, Object[] params ) throws Exception
    {
        // Fetch the Class types of all method parameters
        Class[] types = new Class[params.length];

        for ( int i = 0; i < params.length; i++ )
        {
            types[i] = params[i].getClass();
        }

        return executeMethod( instance, name, types, params );
    }
}
