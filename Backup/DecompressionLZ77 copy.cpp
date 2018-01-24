#include <iostream>
#include <fstream>
#include <list>
#include <string>

using namespace std;

fstream dFile;

// Path of the file to be compressed
string DFilePath()
{
    string filePath = "/Users/eliaspiovani/Documents/OneDrive/Projects/Programming/Codec_Project/Codec/AbstractLoadingCacheTest.ccc";
    //size_t found = filePath.find_last_of('/');
    
    //outputPath = filePath.substr(0, found+1);
    //fileName = filePath.substr(found +1);
    
    return filePath;
}

// Open file that will be compressed
bool DOpenFile()
{
    dFile.open(DFilePath());
    
    if (!dFile)
    {
        cerr << "Unable to open file" << endl;
        return false;
    }
    
    return true;
}

bool DecompressionLZ77 (const int fixedDictLength)
{
    // call function to open the file that will be compressed
    if (!DOpenFile()) {
        cerr << "File not found" << endl;
        return false;
    }
    
    list<char> sWindown;
    list<char>::iterator it;
    
    ofstream output;
    bool endOfFile = false;
    output.open("/Users/eliaspiovani/Documents/OneDrive/Projects/Programming/Codec_Project/Codec/output.java");
    
    //loop for searching through all the file
    while (!endOfFile){
        //for (int i = 0; i <= 64; i++) {
        char lookAhead;
        string position, length = "";
        char character;
        
        if (!dFile.eof()) dFile.get(lookAhead);
        else break;
            
        if (lookAhead == '(') {
            dFile.get(lookAhead);
            while (lookAhead != ',') {
                position += lookAhead;
                dFile.get(lookAhead);
            }
            dFile.get(lookAhead);
            while (lookAhead != ')') {
                length += lookAhead;
                dFile.get(lookAhead);
            }
            dFile.get(lookAhead);
            character = lookAhead;
            //sWindown.push_front(character);
            
            //cout << position << length << character;
            
            if (position == "0" && length == "0") {
                output << character;
                //sWindown.push_front(character);
            }
            else if (length != "0")
            {
                it = sWindown.begin();
                for (int i = 0; i < stoi(position); i++) {
                    ++it;
                }
                for (int i = 0; i < stoi(length); i++) {
                    
                    cout << *it;
                    output << *it;
                    sWindown.push_front(*it);
                    --it;
                }
                output << character;
            }
            sWindown.push_front(character);
            
        }
        
    while (sWindown.size() > fixedDictLength) {
        sWindown.pop_back();
    }

    }
    
    cout << "Search Windown:" << endl;
    for (list<char>::iterator it2 = sWindown.begin(); it2 != sWindown.end(); ++it2) {
        cout << *it2;
    }
    
    return true;
}
