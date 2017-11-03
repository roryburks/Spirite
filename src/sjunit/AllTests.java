package sjunit;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import sjunit.file.BuildSaveLoadTest;
import sjunit.image_data.animations.FFAnimTest;
import sjunit.util.CompactionTests;
import sjunit.util.InterpolationTests;

@RunWith(Suite.class)
@SuiteClasses({ 
	Test1.class, 
	BuildSaveLoadTest.class, 
	FFAnimTest.class ,
	CompactionTests.class,
	InterpolationTests.class,
	ImageTest.class,
	BasicImageTest.class
})
public class AllTests {

}
