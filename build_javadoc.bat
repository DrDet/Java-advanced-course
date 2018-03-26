SET test=artifacts\JarImplementorTest.jar
SET stuff=java\info\kgeorgiy\java\advanced\implementor\
SET link=https://docs.oracle.com/javase/8/docs/api/
SET package=ru.ifmo.rain.vaksman.implementor

javadoc -d javadoc -link %link% -cp java\;lib\*;%test%; -quiet -private -author %package% %stuff%Impler.java %stuff%JarImpler.java %stuff%ImplerException.java
pause