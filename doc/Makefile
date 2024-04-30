.SUFFIXES: .sgml .pdf
OBJS= ivy-java ivy-java.pdf

all: $(OBJS)

.sgml.pdf:
	db2pdf $<

ivy-java: ivy-java.sgml
	rm -fR ivy-java
	db2html $<

clean:
	rm -fR $(OBJS)
