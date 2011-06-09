/*
 * include the headers required by the generated cpp code
 */
%{
#include "Processor.h"
#include "image_pool.h"
using namespace cv;
%}

//import the android-cv.i file so that swig is aware of all that has been previous defined
//notice that it is not an include....
%import "android-cv.i"

//make sure to import the image_pool as it is 
//referenced by the Processor java generated
//class
%typemap(javaimports) Processor "
import com.opencv.jni.image_pool;// import the image_pool interface for playing nice with
								 // android-opencv

/** Processor - for processing images that are stored in an image pool
*/"

class Processor {
public:
	Processor();
	virtual ~Processor();

	int detectAndDrawFeatures(int idx, image_pool* pool);

  bool loadDescription(const char* filename);
};
