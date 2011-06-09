/*
 * Processor.h
 *
 *  Created on: Jun 13, 2010
 *      Author: ethan
 */

#ifndef PROCESSOR_H_
#define PROCESSOR_H_

#include <opencv2/opencv.hpp>
#include <android/log.h>

#include <fstream>
#include <vector>

#include "image_pool.h"

const double THRESHOLD = 0.3;

class Processor
{
public:

  Processor();
  virtual ~Processor();

  /** 指定された画像の特徴点を検出し、LSH特定で物体認識を行う */
  int detectAndDrawFeatures(int idx, image_pool* pool);

  /** 指定されたファイル名から特徴ベクトルをLSHメモリハッシュに読み込む */
  bool loadDescription(const char* filename);

private:  

  int getObjectId (int index);
  
  /** 検出する特徴ベクトルの次元数 */
  static const int DIM = 128;
  /** SURF検出器　*/
  cv::SurfDescriptorExtractor surfex;
  /** LSHメモリハッシュ */
  CvLSH* lsh;
  
  /** 追加された物体の数 */
  int objNumber;
  /** 特徴点累計の最後の添字 */
  int lastIndex;
  /** 各オブジェクトにおける最後の特徴点累計添字 */
  vector<int> lastIndexes;
  /** 各オブジェクトの特徴点数 */
  vector<int> numKeypoints;

};

#endif /* PROCESSOR_H_ */
