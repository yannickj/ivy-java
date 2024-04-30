# Makefile for Ivy test class suite
# it's a bit messy to automate because of specific network settings
# you'll have to play with the network settings to make it run

include ../java.mk # in this file you can change your java compiler and VM
IVYPATH=../build/jar/ivy-java.jar
CLASSPATH=-classpath classes:$(IVYPATH):$(GNUPATH)
SRC = *.java	

# Warning TestNetSwing.java  cant build with jdk1.1
# as Color(r,g,b,a) doesn't exist in jdk1.1

#LOCALNET=10.192.36.255:3456
#LOCALNET=10.0.0.255:3456
#LOCALNET=192.168.19:3456
LOCALNET=
MULTICAST=224.5.6.7:8910
#DOMAIN=
DOMAIN=-b $(MULTICAST)
DEBUG=
#DEBUG= -DIVY_DEBUG

all:
	/bin/rm -fR --  classes
	mkdir classes
	$(JAVAC) -d classes $(JAVACOPTS) $(CLASSPATH) $(SRC)
	@echo "all modules built. run make run"

run: sendnowself nl api filter unitaires probe stop request test1 test2 async
fail: sendnow
	
request:
	@echo "*****************************"
	@echo "TEST REQUEST"
	@echo "*****************************"
	$(JAVA) $(DEBUG) $(JAVAOPTS) $(CLASSPATH) Request $(DOMAIN)
	@echo "*****************************"
	@echo "TEST REQUEST successful"
	@echo "*****************************"
	@echo "\n\n\n"
	
sendnowself:
	@echo "*****************************"
	@echo "TEST SENDNOW SELF"
	@echo "*****************************"
	$(JAVA) $(DEBUG) $(JAVAOPTS) $(CLASSPATH) SendNowSelf $(DOMAIN)
	@echo "*****************************"
	@echo "TEST SENDNOW SELF successful"
	@echo "*****************************"
	@echo "\n\n\n"
	
sendnow:
	@echo "*****************************"
	@echo "TEST SENDNOW"
	@echo "*****************************"
	$(JAVA) $(DEBUG) $(JAVAOPTS) $(CLASSPATH) SendNow $(DOMAIN)
	@echo "*****************************"
	@echo "TEST SENDNOW successful"
	@echo "*****************************"
	@echo "\n\n\n"
	
nl:
	@echo "*****************************"
	@echo "TEST NEWLINE"
	@echo "*****************************"
	$(JAVA) $(DEBUG) $(JAVAOPTS) $(CLASSPATH) NewLine -n 100000 $(DOMAIN)
	@echo "*****************************"
	@echo "TEST NEWLINE successful"
	@echo "*****************************"
	@echo "\n\n\n"

probe:
	@echo "*****************************"
	@echo "TEST PROBE < /dev/null"
	@echo "*****************************"
	$(JAVA) $(DEBUG) $(JAVAOPTS) $(CLASSPATH) fr.dgac.ivy.tools.Probe  $(DOMAIN) < /dev/null
	@echo "*****************************"
	@echo "TEST PROBE SUCCESSFUL"
	@echo "*****************************"
	@echo "\n\n\n"


api:
	@echo "*****************************"
	@echo "TEST API"
	@echo "*****************************"
	$(JAVA) $(DEBUG) $(JAVAOPTS) $(CLASSPATH) TestApi $(DOMAIN)
	@echo "*****************************"
	@echo "TEST API SUCCESSFUL"
	@echo "*****************************"
	@echo "\n\n\n"


async: 
	@echo "*****************************"
	@echo "TEST ASYNC"
	@echo "*****************************"
	$(JAVA) $(DEBUG) $(JAVAOPTS) $(CLASSPATH) AsyncAPI -r -l 5 -d 1 -s 10 $(DOMAIN)
	@echo "*****************************"
	@echo "TEST ASYNC SUCCESSFUL"
	@echo "*****************************"
	@echo "\n\n\n"

stop: 
	@echo "*****************************"
	@echo "TEST STOP"
	@echo "*****************************"
	$(JAVA) $(DEBUG) $(JAVAOPTS) $(CLASSPATH) StopStart $(DOMAIN)
	@echo "*****************************"
	@echo "TEST STOP SUCCESSFUL"
	@echo "*****************************"
	@echo "\n\n\n"

unitaires:
	@echo "*****************************"
	@echo "TEST UNITAIRE"
	@echo "*****************************"
	$(JAVA) $(DEBUG) $(JAVAOPTS) $(CLASSPATH) Unitaire $(DOMAIN)
	@echo "*****************************"
	@echo "TEST UNITAIRE SUCCESSFUL"
	@echo "*****************************"
	@echo "\n\n\n"

filter:
	@echo "*****************************"
	@echo "TEST FILTER MESSAGES"
	@echo "*****************************"
	$(JAVA) $(DEBUG) $(JAVAOPTS) $(CLASSPATH) Filter $(DOMAIN)
	@echo "*****************************"
	@echo "TEST FILTER SUCCESSFUL"
	@echo "*****************************"
	@echo "\n\n\n"

test1: $(OBJ)
	@echo "*****************************"
	@echo "TEST Bench"
	@echo "*****************************"
	$(JAVA) $(DEBUG) $(JAVAOPTS) $(CLASSPATH) BenchLocal $(DOMAIN)
	$(JAVA) $(DEBUG) $(JAVAOPTS) $(CLASSPATH) BenchLocal -d 100 $(DOMAIN)
	$(JAVA) $(DEBUG) $(JAVAOPTS) $(CLASSPATH) BenchLocal -d 0 $(DOMAIN)
	@echo "*****************************"
	@echo "TEST Bench SUCCESSFUL"
	@echo "*****************************"
	@echo "\n\n\n"

test2: $(OBJ)
	@echo "*****************************"
	@echo "TEST Bench 2"
	@echo "*****************************"
	$(JAVA) $(DEBUG) $(JAVAOPTS) $(CLASSPATH) BenchLocal -t 2 -d 100 $(DOMAIN)
	$(JAVA) $(DEBUG) $(JAVAOPTS) $(CLASSPATH) BenchLocal -t 2 -d 0 $(DOMAIN)
	@echo "*****************************"
	@echo "TEST Bench 2 SUCCESSFUL"
	@echo "*****************************"
	@echo "\n\n\n"

swing: $(OBJ)
	@echo "*****************************"
	@echo "TEST Swing "
	@echo "WARNING, this is manual, you have to use the GUI to test ..."
	@echo "TODO, add java.awt.Robot support"
	@echo "*****************************"
	$(JAVA) $(DEBUG) $(JAVAOPTS) $(CLASSPATH) SwingIvy $(DOMAIN)
	@echo "*****************************"
	@echo "TEST Swing Successful"
	@echo "*****************************"
	@echo "\n\n\n"

clean:
	/bin/rm -fR $(OBJ) *.class classes
	@mkdir classes
