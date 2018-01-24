#include <iostream>

using namespace std;

bool GetDict(string *dict, const int fixedDictLength);
bool CompressionLZ77(const int fixedDictLength);
bool DecompressionLZ77(const int fixedDictLength);


int main() {
    const int fixedDictLength = 64;
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
    if (!CompressionLZ77(fixedDictLength))
    {
        return 100;
    }
    
    if (!DecompressionLZ77(fixedDictLength))
    {
        return 300;
    }
    
}
