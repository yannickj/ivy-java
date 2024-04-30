include ../java.mk

.SUFFIXES: .java .class
     SRCS = TestIvy.java TestIvySwing.java Counter.java ivyTranslater.java Translate.java TranslateXML.java EndApp.java
     OBJS = $(SRCS:.java=.class)

BUILDPATH=../ivy-java.jar

.java.class:
	$(JAVAC) $(JAVACOPTS) -classpath $(BUILDPATH) $<

all:	$(OBJS)

clean:
	/bin/rm -f -- $(OBJS) *~ *.bak *.class
