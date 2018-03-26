cd java
javac -d .. -cp ..\artifacts\JarImplementorTest.jar -Xlint ru\ifmo\rain\vaksman\implementor\Implementor.java ru\ifmo\rain\vaksman\implementor\CodeGenerator.java ru\ifmo\rain\vaksman\implementor\UnicodeWriter.java
cd ..
jar xf artifacts\JarImplementorTest.jar info\kgeorgiy\java\advanced\implementor\Impler.class info\kgeorgiy\java\advanced\implementor\JarImpler.class info\kgeorgiy\java\advanced\implementor\ImplerException.class
jar cvfm Implementor.jar manifest.mf ru\ifmo\rain\vaksman\implementor\*.class info\kgeorgiy\java\advanced\implementor\*.class
rmdir info /s
rmdir ru /s