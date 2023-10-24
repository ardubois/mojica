]all: javacc java

javacc: Mojica.jj
	javacc Mojica.jj

java: 
	javac *.java
	javac ast/*.java
	javac inter/*.java
	javac tc/*.java
	javac llvm/*.java

clean:
	rm *.java 
	rm *.class
