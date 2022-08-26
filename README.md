# Reconstruct
Resconstruct is a java library to infer missing information vectors of java classes.  

## Features
  - Phantom classes
    - Inheritance solving
    - Dummy field and method creation

## Usage
```java
byte[] classData = ...;

Reconstruct re = new Reconstruct();
re.add(classData);
re.run();
// now all phantoms that were generated for this class will be generated
// you can either get the class hireachy 
ClassHireachy ch = re.getHireachy();
List<PhantomClass> phantoms = ch.getPhantoms();
... // do something
// or convert them to class bytes
Map<String, byte[]> builtClasses = re.build();
... // do something
```
Reconstruct throws `GenerateException` when the generation is impossible to complete, but errors can be ignored via `setIgnoreSolveExceptions`
