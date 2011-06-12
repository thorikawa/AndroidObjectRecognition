#include <iostream>
#include <fstream>

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/features2d/features2d.hpp>

using namespace std;

const int DIM_VECTOR = 128;  // 128次元ベクトル

/**
 * SURF情報をファイルに出力
 * copy from http://d.hatena.ne.jp/aidiary/20091030/1256905218
 * @param[in]   imageDescriptors    SURF特徴ベクトル情報
 * @return なし
 */
void writeSURF(CvSeq* imageKeypoints, CvSeq* imageDescriptors) {
  for (int i = 0; i < imageKeypoints->total; i++) {
    CvSURFPoint* point = (CvSURFPoint*)cvGetSeqElem(imageKeypoints, i);
    float* descriptor = (float*)cvGetSeqElem(imageDescriptors, i);
    // キーポイント情報（X座標, Y座標, サイズ, ラプラシアン）を書き込む
    // 特徴ベクトルを書き込む
    for (int j = 0; j < DIM_VECTOR; j++) {
      printf("%f\t", descriptor[j]);
    }
    cout << endl;
  }
}

int main(int argc, char** argv) {
  if (argc < 1) {
    cerr << "usage:" << endl;
    cerr << " a.out input.tsv < example.jpg" << endl;
    return -1;
  }

  const char* imageFile = argc == 2 ? argv[1] : "image/accordion_image_0001.jpg";

  // SURF抽出用に画像をグレースケールで読み込む
  IplImage* grayImage = cvLoadImage(imageFile, CV_LOAD_IMAGE_GRAYSCALE);
  if (!grayImage) {
    cerr << "cannot find image file: " << imageFile << endl;
    return -1;
  }

  CvMemStorage* storage = cvCreateMemStorage(0);
  CvSeq* imageKeypoints = 0;
  CvSeq* imageDescriptors = 0;
  CvSURFParams params = cvSURFParams(500, 1);
  
  // 画像からSURFを取得
  cvExtractSURF(grayImage, 0, &imageKeypoints, &imageDescriptors, storage, params);
  cerr << "Image Descriptors: " << imageDescriptors->total << endl;
  
  // SURFをファイルに出力
  writeSURF(imageKeypoints, imageDescriptors);
  
  // 後始末
  cvReleaseImage(&grayImage);
  cvClearSeq(imageKeypoints);
  cvClearSeq(imageDescriptors);
  cvReleaseMemStorage(&storage);
  cvDestroyAllWindows();
  
  return 0;
}
