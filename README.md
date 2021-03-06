# Spirite

Spirite is an Image Creation/Editing Software built in Java/Swing, using JPen 2 for tablet input, and JOGL to hardware accelerate certain rendering functons.  

Though it is a fully-functional, multi-purpose image editor with many modern capabilities, Spirite is designed particularly for creating and editing pixel-based sprites, with a particular emphasis on animation.  

Key Features related to this design intent:
-A robust palette scheme allowing the dragging of palette colors to arrange them in a visually-intuitive way and the ability to quickly save and load palettes.
-A reference scheme which allows you to drag and resize reference images (such as rough sketches drawn at a much higher size) and display them over or under the drawing region without a loss in image quality or cumbersomely editing the image in order to get the same effect.
-An animation preview panel and an animation scheme editor which updates in real time as you edit the image and automatically binds to a subgroup folder so that you don't have to manually edit the animation each time you plus a new frame or swap frames around.
-The ability to create _layers made up of multiple minus-parts which can be individually manipulated and the ability to export the animations in a way that preserves the pieces as well as the fully-constructed animation.

# Building
Steps for getting the project set up in a Java IDE:

1. Download and import these external libraries:
    + (a) Apache Commons [https://commons.apache.org/proper/commons-collections/download_collections.cgi]
        + BidiMap is the only one used, but it's easier to import the whole thing.
    + (b) JOGL, [https://jogamp.org/wiki/index.php/Downloading_and_installing_JOGL]
        + jogl-all
        + jogl {natives for the platform(s) you're arranged for
        + gluegen-rt
        + gluegen-rt {natives}
    + (_color) Java OpenGL Mathematics Library [https://github.com/jroyalty/jglm]
    + (d) JPen [https://sourceforge.net/projects/jpen/]
2. Link JUnit4
3. Make sure the Resource folder (Spirite/res) is linked through the IDE
