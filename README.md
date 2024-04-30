# ivy-java
Ivy software Bus implementation in the java language


  Ivy java is open source software distributed under the terms of the GNU
  Lesser General Public License (LGPL).  See the COPYING.LIB file for details.
  Some included utilities are distributed under the terms of the GNU
  General Public License, a copy of which is included in the file COPYING.

  This page has been hugely inspired from the one Wes wrote for the gnu-regexp
  package. As I am a newbie in package creation, I started from an existing one.

COMPILING

  you should have somewhere: ant, gnu-getopt.jar, java-servlet.jar
  then ant should build the ivy-java.jar file in the build/jar directory

INSTALLING

  Copy the ivy-java.jar file (located in the 'build/jar' directory)
  to your usual installation directory for Java archives. If it is located in
  your main java class repository, it is possible that will just work as is,
  however, you might want to put it elsewhere and fiddle with the CLASSPATH
  environment variable.

  Typically this is done by adding an entry to your CLASSPATH
  variable setting with the full path to the JAR file, e.g.
  csh:  % setenv CLASSPATH ${CLASSPATH}:/usr/java/lib/ivy-java.jar
  bash:  % export CLASSPATH=${CLASSPATH}:/usr/java/lib/ivy-java.jar
  DOS:  > set CLASSPATH %CLASSPATH%;C:\Java\lib\ivy-java.jar
  Various shells and operating systems may have slightly different methods.
  Consult your Java virtual machine documentation for details.  You may also
  specify the -classpath option to the java executable, e.g.
  compile: % javac -classpath /usr/java/lib/ivy-java.jar MyClass.java
  execute: % java -classpath /usr/java/lib/ivy-java.jar MyClass

DOCUMENTATION

  ivy-java should come with
  - one man page: doc/ivy-java.1,
  - the javadoc api html documentation tree: doc/api/
  - a programmer's guide both in html: doc/programmersguide/ 
    and in pdf format: doc/programmersguide.pdf 

  If any of those file is missing, see the tar.gz archive on the Ivy java
  web page ( http://www2.tls.cena.fr/products/ivy/ivy-java.html )

UTILITIES

  ivy-java comes with a simple utility program intended to test
  and demonstrate its features. It is compiled into the Java archive
  file. To run fr.dgac.ivy.tools.Probe, you will need gnu.getopt,
  which is available at http://www.urbanophile.com/~arenn/hacking/download.html,
  and put those class files in your classpath as well. 

  Running java fr.dgac.ivy.Probe successfully is the key to knowing whether
  your ivy-java installation is OK.

  Ivy also comes with a simple TCP relay, allowing any script application to
  send text messages onto an Ivy bus. To run the relay, launch
  $ java fr.dgac.ivy.tools.IvyDaemon
  Then any line sent to the local port 3456 will be forwarded as an ivy
  message. It can be used in shell scripts in conjunction with netcat
  $ echo "hello world" | nc -q 0 localhost 3456

HACKING

  You are free to fold, spindle, mutilate and modify this library,
  provided you follow the terms outlined in COPYING.LIB.  The ivy-java
  project team gratefully accepts any bug fixes or enhancements you may
  come up with (see the TODO file if you're in need of some ideas). A
  few parameters at the top of the Makefile in the 'src' directory
  need to be edited to match your local system setup.

BUG REPORTS

  Send bug reports to <yannick.jestin@gmail.com>
  It helps if you can send a code sample showing the messages you were
  using and how you were using it.

LATEST VERSION

  You can always obtain info about the latest version of ivy-java at
  http://www.tls.cena.fr/products/ivy/download/desc/ivy-java.html.
  Don't hesitate to ask me by mail a cvs snapshot if you're not satified with
  the upstream release.


Thanks!

-- 
  Yannick Jestin <yannick.jestin@gmail.com>
