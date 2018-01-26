#include <iostream>

using namespace std;

bool GetDict(string *dict, const int fixedDictLength);
bool CompressionLZ77(const int fixedDictLength);
bool DecompressionLZ77(const int fixedDictLength);
bool C_Huffman();
bool D_Huffman();

int main() {
    const int fixedDictLength = 99;
    
    // set the maximum length for dictionary and slider window
    string dictionary[fixedDictLength];
    // call function to get dictionary from txt file
    
    if (!GetDict(dictionary, fixedDictLength))
    {
        //for (int i = 0; i <= fixedDictLength -1; i++) {
        //    cout << dictionary[i] << endl;
        //}
        cerr << "Unable to load Dictionary";
        return 200;
    }
    
    // realize the compression of the file
//    if (!CompressionLZ77(fixedDictLength))
//    {
//        return 100;
//    }
    
    //Huffman compression
//    if (!C_Huffman())
//    {
//        return 400;
//    }
    
    //Huffman decompression
    if (!D_Huffman())
    {
        return 500;
    }
    
//    if (!DecompressionLZ77(fixedDictLength))
//    {
//        return 300;
//    }
    
    /*//Delete files used to compress the document
    if( remove("/Users/eliaspiovani/Documents/OneDrive/Projects/Programming/Codec_Project/Codec/AbstractLoadingCacheTest.lz77") != 0 )
        cerr << "Error deleting file";
    else
        cerr << "File successfully deleted";
    return 0;*/
    
}
