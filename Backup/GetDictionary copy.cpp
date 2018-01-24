#include <iostream>
#include <fstream>
#include <list>

using namespace std;

// path where is the file that contains the dictionary
string DictPath ()
{
    string dictPath = "/Users/eliaspiovani/Documents/OneDrive/Projects/Programming/Codec_Project/Codec/Vocabulary.txt";
    return dictPath;
}

// Function to open the dictionary file and save it into an array of strings
bool GetDict(string *dict, const int fixedDictLength)
{
    fstream vocabulary;
    vocabulary.open(DictPath());
    
    cout << DictPath() << endl;
    
    if (!vocabulary) {
        cerr << "Unable to open the vocabulary file" << endl;
        return false;
    }
    
    string line;
    //string vocab[64];
    int i = 0;
    if (vocabulary.is_open()) {
        while(getline(vocabulary, line))
        {
            //cout << line << endl;
            *dict = line;
            dict++;
            i++;
            if (i > fixedDictLength - 1) break;
            
        }
        vocabulary.close();
        return true;
    }
    return false;
}

