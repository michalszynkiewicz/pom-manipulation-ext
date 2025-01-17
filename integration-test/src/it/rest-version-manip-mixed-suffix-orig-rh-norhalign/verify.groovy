/**
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


def pomFile = new File( basedir, 'pom.xml' )
System.out.println( "Slurping POM: ${pomFile.getAbsolutePath()}" )

def pom = new XmlSlurper().parse( pomFile )

System.out.println( "POM Version: ${pom.version.text()}" )
assert pom.version.text().equals( '1.0.0.temporary-redhat-2' )

// Currently the AddSuffixJettyHandler doesn't do OSGi compatibility.
def dependency = pom.dependencyManagement.dependencies.dependency.find { it.artifactId.text() == "commons-lang" }
assert dependency != null
assert dependency.version.text() == "1.0.0.redhat-1"

dependency = pom.dependencies.dependency.find { it.artifactId.text() == "errai-common" }
assert dependency != null
assert dependency.version.text() == "1.1-Final-temporary-redhat-1"

def passed = 0
pom.properties.children().each {
    if ( it.text().contains ("3.1.0-temporary-redhat-1") && it.name() == "httpclient" )
    {
        passed++
    }
    if ( it.text().contains ("1.1-Final-redhat-1") && it.name() == "errai-tools" )
    {
        passed++
    }
}
assert (passed == 2)
